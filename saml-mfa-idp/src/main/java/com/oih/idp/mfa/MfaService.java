package com.oih.idp.mfa;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.oih.idp.audit.AuditService;
import com.oih.idp.config.IdpProperties;
import com.oih.idp.identity.IdentityException;
import com.oih.idp.identity.domain.User;
import com.oih.idp.identity.repo.UserRepository;
import com.oih.idp.policy.Argon2PasswordHasher;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MfaService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TotpService totp;
    private final EnvelopeCrypto envelope;
    private final MfaSecretRepository secrets;
    private final RecoveryCodeRepository recoveryCodes;
    private final Argon2PasswordHasher hasher;
    private final UserRepository users;
    private final IdpProperties properties;
    private final AuditService audit;

    public MfaService(
            TotpService totp,
            EnvelopeCrypto envelope,
            MfaSecretRepository secrets,
            RecoveryCodeRepository recoveryCodes,
            Argon2PasswordHasher hasher,
            UserRepository users,
            IdpProperties properties,
            AuditService audit) {
        this.totp = totp;
        this.envelope = envelope;
        this.secrets = secrets;
        this.recoveryCodes = recoveryCodes;
        this.hasher = hasher;
        this.users = users;
        this.properties = properties;
        this.audit = audit;
    }

    public boolean platformEnabled() {
        return properties.getMfa().isEnabled();
    }

    public boolean isEnrolled(UUID userId) {
        return secrets.findById(userId).map(MfaSecret::isConfirmed).orElse(false);
    }

    @Transactional
    public Enrollment startEnrollment(User user) {
        byte[] secret = totp.generateSecret();
        MfaSecret row = secrets.findById(user.getId()).orElseGet(() -> new MfaSecret(user.getId(), new byte[0]));
        row.replaceCiphertext(envelope.encrypt(secret));
        secrets.save(row);
        recoveryCodes.deleteAll(recoveryCodes.findByUserIdAndConsumedAtIsNull(user.getId()));
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < properties.getMfa().getRecoveryCodeCount(); i++) {
            String code = randomCode();
            codes.add(code);
            recoveryCodes.save(new RecoveryCode(user.getId(), hasher.hash(code)));
        }
        user.setMfaEnabled(false);
        users.save(user);
        String uri = totp.otpauth(properties.getDisplayName(), user.getUsername(), secret);
        return new Enrollment(uri, qrPng(uri), codes, secret);
    }

    @Transactional
    public void confirmEnrollment(User user, String code) {
        MfaSecret row = secrets.findById(user.getId()).orElseThrow(() -> new IdentityException("No enrollment in progress."));
        byte[] secret = envelope.decrypt(row.getSecretCiphertext());
        if (!totp.verify(secret, code, null)) {
            audit.record("MFA_ENROLL", "FAILURE", user.getId(), null, "BAD_TOTP");
            throw new IdentityException("Invalid authenticator code.");
        }
        row.confirm();
        row.setLastTimestep(totp.currentTimestep());
        secrets.save(row);
        user.setMfaEnabled(true);
        users.save(user);
        audit.record("MFA_ENROLL", "SUCCESS", user.getId(), null, null);
    }

    @Transactional
    public boolean verifyTotp(User user, String code) {
        MfaSecret row = secrets.findById(user.getId()).orElse(null);
        if (row == null || !row.isConfirmed()) {
            return false;
        }
        byte[] secret = envelope.decrypt(row.getSecretCiphertext());
        if (!totp.verify(secret, code, row.getLastTimestep())) {
            audit.record("MFA", "FAILURE", user.getId(), null, "BAD_TOTP");
            return false;
        }
        row.setLastTimestep(totp.currentTimestep());
        secrets.save(row);
        audit.record("MFA", "SUCCESS", user.getId(), null, null);
        return true;
    }

    @Transactional
    public boolean consumeRecoveryCode(User user, String code) {
        for (RecoveryCode stored : recoveryCodes.findByUserIdAndConsumedAtIsNull(user.getId())) {
            if (hasher.matches(code, stored.getCodeHash())) {
                stored.consume();
                recoveryCodes.save(stored);
                if (properties.getMfa().isRecoveryRequiresReset()) {
                    user.setMfaEnabled(false);
                    secrets.findById(user.getId()).ifPresent(secrets::delete);
                    users.save(user);
                }
                audit.record("MFA", "SUCCESS", user.getId(), null, "RECOVERY");
                return true;
            }
        }
        audit.record("MFA", "FAILURE", user.getId(), null, "BAD_RECOVERY");
        return false;
    }

    private static String randomCode() {
        byte[] bytes = new byte[5];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String qrPng(String uri) {
        try {
            var matrix = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("QR generation failed", ex);
        }
    }

    public record Enrollment(String otpauthUri, String qrPngBase64, List<String> recoveryCodes, byte[] secret) {
    }
}
