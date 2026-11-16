package com.oih.idp.crypto;

import com.oih.idp.config.IdpProperties;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AuthProvider;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "idp.crypto.token", havingValue = "pkcs11")
public class Pkcs11CryptoToken implements CryptoToken {

    private final IdpProperties.Crypto settings;
    private final ConcurrentHashMap<String, String> aliases = new ConcurrentHashMap<>();
    private Provider provider;
    private KeyStore keyStore;

    public Pkcs11CryptoToken(IdpProperties properties) {
        this.settings = properties.getCrypto();
    }

    @PostConstruct
    void init() throws Exception {
        Path library = Path.of(settings.getLibrary());
        if (!Files.isReadable(library)) {
            throw new IllegalStateException("PKCS#11 library is not readable: " + library);
        }
        Path cfg = Files.createTempFile("oih-pkcs11-", ".cfg");
        Files.writeString(
                cfg,
                """
                name = OihIdp
                library = %s
                slotListIndex = %d
                """
                        .formatted(library.toAbsolutePath(), settings.getSlotListIndex()),
                StandardCharsets.UTF_8);
        provider = Security.getProvider("SunPKCS11");
        if (provider == null) {
            throw new IllegalStateException("SunPKCS11 provider is not available");
        }
        provider = provider.configure(cfg.toAbsolutePath().toString());
        Security.addProvider(provider);
        keyStore = KeyStore.getInstance("PKCS11", provider);
        char[] pin = settings.getPin() == null ? new char[0] : settings.getPin().toCharArray();
        keyStore.load(null, pin);
        if (provider instanceof AuthProvider authProvider && pin.length > 0) {
            authProvider.login(null, new javax.security.auth.callback.CallbackHandler() {
                @Override
                public void handle(javax.security.auth.callback.Callback[] callbacks) {
                    for (javax.security.auth.callback.Callback callback : callbacks) {
                        if (callback instanceof javax.security.auth.callback.PasswordCallback passwordCallback) {
                            passwordCallback.setPassword(pin);
                        }
                    }
                }
            });
        }
    }

    @Override
    public GeneratedKey generateSigningKey(String label, String ckaId, int keySize) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", provider);
            generator.initialize(keySize);
            KeyPair pair = generator.generateKeyPair();
            aliases.put(ckaId, label);
            return new GeneratedKey(ckaId, label, pair.getPrivate(), pair.getPublic());
        } catch (Exception ex) {
            throw new IllegalStateException("PKCS#11 key generation failed", ex);
        }
    }

    public void storeCertificate(String ckaId, String label, PrivateKey privateKey, X509Certificate certificate) {
        try {
            char[] pin = settings.getPin() == null ? new char[0] : settings.getPin().toCharArray();
            keyStore.setKeyEntry(label, privateKey, pin, new Certificate[] {certificate});
            aliases.put(ckaId, label);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to bind certificate in PKCS#11 token", ex);
        }
    }

    @Override
    public void persistCertificate(String ckaId, String label, PrivateKey privateKey, X509Certificate certificate) {
        storeCertificate(ckaId, ckaId, privateKey, certificate);
    }

    @Override
    public boolean contains(String ckaId) {
        try {
            String alias = aliases.getOrDefault(ckaId, ckaId);
            return keyStore.getKey(alias, settings.getPin().toCharArray()) instanceof PrivateKey;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public PrivateKey privateKey(String ckaId) {
        try {
            String alias = aliases.getOrDefault(ckaId, ckaId);
            Key key = keyStore.getKey(alias, settings.getPin().toCharArray());
            if (key instanceof PrivateKey privateKey) {
                return privateKey;
            }
            throw new IllegalStateException("No private key for " + ckaId);
        } catch (Exception ex) {
            throw new IllegalStateException("PKCS#11 private key lookup failed", ex);
        }
    }

    @Override
    public PublicKey publicKey(String ckaId) {
        try {
            String alias = aliases.getOrDefault(ckaId, ckaId);
            Certificate cert = keyStore.getCertificate(alias);
            if (cert != null) {
                return cert.getPublicKey();
            }
            throw new IllegalStateException("No public key for " + ckaId);
        } catch (Exception ex) {
            throw new IllegalStateException("PKCS#11 public key lookup failed", ex);
        }
    }

    @Override
    public boolean isExtractable(PrivateKey privateKey) {
        return privateKey.getEncoded() != null;
    }

    public Provider provider() {
        return provider;
    }
}
