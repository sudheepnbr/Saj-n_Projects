package com.oih.idp.crypto;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public final class SelfSignedTokenCertificate {

    private SelfSignedTokenCertificate() {
    }

    public static X509Certificate issue(String commonName, PublicKey publicKey, PrivateKey signingKey) {
        try {
            Instant now = Instant.now();
            X500Name name = new X500Name("CN=" + commonName);
            PrivateKey material = signingKey instanceof NonExtractablePrivateKey wrapped
                    ? wrapped.unwrap()
                    : signingKey;
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(material);
            var builder = new JcaX509v3CertificateBuilder(
                    name,
                    BigInteger.valueOf(now.toEpochMilli()),
                    Date.from(now.minus(1, ChronoUnit.MINUTES)),
                    Date.from(now.plus(3650, ChronoUnit.DAYS)),
                    name,
                    publicKey);
            return new JcaX509CertificateConverter()
                    .setProvider(new BouncyCastleProvider())
                    .getCertificate(builder.build(signer));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to bind X.509 certificate to token public key", ex);
        }
    }
}
