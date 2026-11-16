package com.oih.idp.mfa;

import com.oih.idp.config.IdpProperties;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TotpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final IdpProperties.Mfa settings;

    public TotpService(IdpProperties properties) {
        this.settings = properties.getMfa();
    }

    public byte[] generateSecret() {
        byte[] secret = new byte[20];
        RANDOM.nextBytes(secret);
        return secret;
    }

    public boolean verify(byte[] secret, String code, Long lastTimestep) {
        if (code == null || !code.matches("\\d{" + settings.getDigits() + "}")) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000L / settings.getPeriodSeconds();
        int window = settings.getWindow();
        for (long step = now - window; step <= now + window; step++) {
            if (lastTimestep != null && step == lastTimestep) {
                continue;
            }
            if (otp(secret, step).equals(code)) {
                return true;
            }
        }
        return false;
    }

    public long currentTimestep() {
        return System.currentTimeMillis() / 1000L / settings.getPeriodSeconds();
    }

    public String otp(byte[] secret, long timestep) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(timestep).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int mod = (int) Math.pow(10, settings.getDigits());
            return String.format("%0" + settings.getDigits() + "d", binary % mod);
        } catch (Exception ex) {
            throw new IllegalStateException("TOTP generation failed", ex);
        }
    }

    public String otpauth(String issuer, String username, byte[] secret) {
        return "otpauth://totp/"
                + url(issuer) + ":" + url(username)
                + "?secret=" + base32(secret)
                + "&issuer=" + url(issuer)
                + "&period=" + settings.getPeriodSeconds()
                + "&digits=" + settings.getDigits()
                + "&algorithm=SHA1";
    }

    static String base32(byte[] data) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                out.append(alphabet.charAt((buffer >> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            out.append(alphabet.charAt((buffer << (5 - bitsLeft)) & 31));
        }
        return out.toString();
    }

    private static String url(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
