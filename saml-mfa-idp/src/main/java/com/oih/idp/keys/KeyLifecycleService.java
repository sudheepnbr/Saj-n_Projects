package com.oih.idp.keys;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.crypto.CryptoToken;
import com.oih.idp.crypto.CryptoToken.GeneratedKey;
import com.oih.idp.crypto.SelfSignedTokenCertificate;
import com.oih.idp.crypto.SigningService;
import com.oih.idp.keys.SigningKey.Status;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Order(0)
public class KeyLifecycleService implements SigningService.KeyLookup, ApplicationRunner {

    private final CryptoToken token;
    private final SigningKeyRepository keys;
    private final IdpProperties properties;

    public KeyLifecycleService(CryptoToken token, SigningKeyRepository keys, IdpProperties properties) {
        this.token = token;
        this.keys = keys;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureActiveKey();
    }

    @Transactional
    public void ensureActiveKey() {
        var active = keys.findByStatus(Status.ACTIVE);
        if (active.isEmpty() || !token.contains(active.get().getCkaId())) {
            active.ifPresent(key -> {
                key.setStatus(Status.DESTROYED);
                keys.saveAndFlush(key);
            });
            generate(Status.ACTIVE, "idp-signing-active");
        }
        SigningKey loaded = requireActive();
        var privateKey = token.privateKey(loaded.getCkaId());
        if (token.isExtractable(privateKey)) {
            throw new IllegalStateException("Active signing key is extractable; refusing to start.");
        }
    }

    @Transactional
    public SigningKey generateNext() {
        var existing = keys.findByStatus(Status.NEXT);
        if (existing.isPresent() && token.contains(existing.get().getCkaId())) {
            return existing.get();
        }
        existing.ifPresent(key -> {
            key.setStatus(Status.DESTROYED);
            keys.saveAndFlush(key);
        });
        return generate(Status.NEXT, "idp-signing-next");
    }

    @Transactional
    public void activateNext() {
        SigningKey next = keys.findByStatus(Status.NEXT)
                .orElseThrow(() -> new IllegalStateException("No NEXT signing key"));
        keys.findByStatus(Status.ACTIVE).ifPresent(active -> {
            active.setStatus(Status.RETIRING);
            keys.saveAndFlush(active);
        });
        next.setStatus(Status.ACTIVE);
        keys.saveAndFlush(next);
    }

    @Override
    public SigningKey requireActive() {
        return keys.findByStatus(Status.ACTIVE).orElseThrow(() -> new IllegalStateException("No ACTIVE signing key"));
    }

    @Override
    public List<SigningKey> published() {
        return keys.findByStatusIn(List.of(Status.ACTIVE, Status.NEXT));
    }

    public List<SigningKey> all() {
        return keys.findAll();
    }

    private SigningKey generate(Status status, String labelPrefix) {
        String ckaId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String label = labelPrefix + "-" + ckaId.substring(0, 8);
        GeneratedKey generated = token.generateSigningKey(label, ckaId, properties.getCrypto().getKeySize());
        if (token.isExtractable(generated.privateKey())) {
            throw new IllegalStateException("Newly generated key is extractable");
        }
        X509Certificate certificate = SelfSignedTokenCertificate.issue(
                properties.getEntityId(), generated.publicKey(), generated.privateKey());
        token.persistCertificate(ckaId, label, generated.privateKey(), certificate);
        SigningKey row = new SigningKey(
                ckaId,
                label,
                status,
                der(certificate),
                fingerprint(certificate),
                certificate.getNotBefore().toInstant(),
                certificate.getNotAfter().toInstant());
        return keys.save(row);
    }

    private static byte[] der(X509Certificate certificate) {
        try {
            return certificate.getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String fingerprint(X509Certificate certificate) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
