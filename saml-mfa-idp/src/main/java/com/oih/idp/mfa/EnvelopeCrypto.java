package com.oih.idp.mfa;

import com.oih.idp.config.IdpProperties;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class EnvelopeCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final byte[] key;

    public EnvelopeCrypto(IdpProperties properties) {
        String configured = properties.getMfa().getEnvelopeKey();
        if (configured == null || configured.isBlank()) {
            byte[] generated = new byte[32];
            RANDOM.nextBytes(generated);
            this.key = generated;
        } else {
            this.key = Base64.getDecoder().decode(configured);
        }
        if (this.key.length != 32) {
            throw new IllegalStateException("OIH_TOTP_WRAP_KEY must be 32 bytes (base64).");
        }
    }

    public byte[] encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return out;
        } catch (Exception ex) {
            throw new IllegalStateException("Envelope encrypt failed", ex);
        }
    }

    public byte[] decrypt(byte[] blob) {
        try {
            byte[] iv = Arrays.copyOfRange(blob, 0, 12);
            byte[] ciphertext = Arrays.copyOfRange(blob, 12, blob.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception ex) {
            throw new IllegalStateException("Envelope decrypt failed", ex);
        }
    }
}
