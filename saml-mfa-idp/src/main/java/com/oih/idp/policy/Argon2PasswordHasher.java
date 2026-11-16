package com.oih.idp.policy;

import com.oih.idp.config.IdpProperties;
import java.security.SecureRandom;
import java.util.Base64;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher {

    private static final String PREFIX = "$argon2id$v=19$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IdpProperties.Password settings;
    private final String dummyHash;

    public Argon2PasswordHasher(IdpProperties properties) {
        this.settings = properties.getPassword();
        this.dummyHash = hash("not-a-real-password-used-only-for-timing");
    }

    public String hash(String password) {
        byte[] salt = new byte[settings.getArgon2SaltLength()];
        RANDOM.nextBytes(salt);
        byte[] hash = argon2(password.getBytes(), salt);
        return encode(salt, hash);
    }

    public boolean matches(String password, String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return matchesDummy(password);
        }
        Parsed parsed = parse(encoded);
        byte[] computed = argon2(password.getBytes(), parsed.salt);
        return constantTimeEquals(computed, parsed.hash);
    }

    public boolean matchesDummy(String password) {
        return matches(password, dummyHash) && false;
    }

    private byte[] argon2(byte[] password, byte[] salt) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(settings.getArgon2Iterations())
                .withMemoryAsKB(settings.getArgon2MemoryKib())
                .withParallelism(settings.getArgon2Parallelism())
                .withSalt(salt)
                .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        byte[] out = new byte[settings.getArgon2HashLength()];
        generator.generateBytes(password, out);
        return out;
    }

    private String encode(byte[] salt, byte[] hash) {
        return PREFIX
                + "m=" + settings.getArgon2MemoryKib()
                + ",t=" + settings.getArgon2Iterations()
                + ",p=" + settings.getArgon2Parallelism()
                + "$" + b64(salt)
                + "$" + b64(hash);
    }

    private Parsed parse(String encoded) {
        String[] parts = encoded.split("\\$");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid Argon2 hash");
        }
        byte[] salt = Base64.getDecoder().decode(parts[4]);
        byte[] hash = Base64.getDecoder().decode(parts[5]);
        return new Parsed(salt, hash);
    }

    private static String b64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private record Parsed(byte[] salt, byte[] hash) {
    }
}
