package com.oih.idp.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "idp.crypto.token", havingValue = "software", matchIfMissing = true)
public class SoftwareCryptoToken implements CryptoToken {

    /** JVM-scoped, like a process-local HSM. Survives Spring test context reloads. */
    private static final ConcurrentHashMap<String, GeneratedKey> KEYS = new ConcurrentHashMap<>();

    @Override
    public GeneratedKey generateSigningKey(String label, String ckaId, int keySize) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(keySize);
            KeyPair pair = generator.generateKeyPair();
            NonExtractablePrivateKey wrapped = new NonExtractablePrivateKey(pair.getPrivate());
            GeneratedKey generated = new GeneratedKey(ckaId, label, wrapped, pair.getPublic());
            KEYS.put(ckaId, generated);
            return generated;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate in-memory token key", ex);
        }
    }

    @Override
    public PrivateKey privateKey(String ckaId) {
        GeneratedKey key = KEYS.get(ckaId);
        if (key == null) {
            throw new IllegalStateException("Unknown token key " + ckaId);
        }
        return key.privateKey();
    }

    @Override
    public PublicKey publicKey(String ckaId) {
        GeneratedKey key = KEYS.get(ckaId);
        if (key == null) {
            throw new IllegalStateException("Unknown token key " + ckaId);
        }
        return key.publicKey();
    }

    @Override
    public boolean contains(String ckaId) {
        return KEYS.containsKey(ckaId);
    }

    @Override
    public boolean isExtractable(PrivateKey privateKey) {
        return privateKey.getEncoded() != null;
    }
}
