package com.oih.idp.crypto;

import com.oih.idp.keys.SigningKey;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SigningService {

    private final CryptoToken token;
    private final KeyLookup keys;

    public SigningService(CryptoToken token, KeyLookup keys) {
        this.token = token;
        this.keys = keys;
    }

    public byte[] sign(byte[] payload) {
        SigningKey active = keys.requireActive();
        PrivateKey privateKey = token.privateKey(active.getCkaId());
        if (token.isExtractable(privateKey)) {
            throw new IllegalStateException("Active signing key is extractable");
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            PrivateKey material = privateKey instanceof NonExtractablePrivateKey wrapped
                    ? wrapped.unwrap()
                    : privateKey;
            signature.initSign(material);
            signature.update(payload);
            return signature.sign();
        } catch (Exception ex) {
            throw new IllegalStateException("Signing failed", ex);
        }
    }

    public boolean verify(byte[] payload, byte[] signatureBytes, X509Certificate certificate) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(certificate);
            signature.update(payload);
            return signature.verify(signatureBytes);
        } catch (Exception ex) {
            return false;
        }
    }

    public X509Certificate certificateOf(SigningKey key) {
        try {
            return (X509Certificate)
                    CertificateFactory.getInstance("X.509")
                            .generateCertificate(new java.io.ByteArrayInputStream(key.getCertificateDer()));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid stored certificate", ex);
        }
    }

    public PrivateKey activePrivateKey() {
        SigningKey active = keys.requireActive();
        PrivateKey privateKey = token.privateKey(active.getCkaId());
        if (token.isExtractable(privateKey)) {
            throw new IllegalStateException("Active signing key is extractable");
        }
        return privateKey instanceof NonExtractablePrivateKey wrapped ? wrapped.unwrap() : privateKey;
    }

    public X509Certificate activeCertificate() {
        return certificateOf(keys.requireActive());
    }

    public List<SigningKey> metadataKeys() {
        return keys.published();
    }

    public interface KeyLookup {
        SigningKey requireActive();

        List<SigningKey> published();
    }
}
