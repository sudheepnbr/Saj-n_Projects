package com.oih.idp.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface CryptoToken {

    GeneratedKey generateSigningKey(String label, String ckaId, int keySize);

    PrivateKey privateKey(String ckaId);

    PublicKey publicKey(String ckaId);

    boolean contains(String ckaId);

    boolean isExtractable(PrivateKey privateKey);

    default void persistCertificate(
            String ckaId, String label, PrivateKey privateKey, java.security.cert.X509Certificate certificate) {
    }

    record GeneratedKey(String ckaId, String label, PrivateKey privateKey, PublicKey publicKey) {
    }
}
