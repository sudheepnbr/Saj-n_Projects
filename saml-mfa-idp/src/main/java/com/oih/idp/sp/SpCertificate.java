package com.oih.idp.sp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sp_certificates")
public class SpCertificate {

    public static final String SIGNING = "SIGNING";
    public static final String ENCRYPTION = "ENCRYPTION";

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sp_id")
    private ServiceProvider serviceProvider;

    @Column(nullable = false, length = 16)
    private String usage;

    @Column(name = "certificate_der", nullable = false)
    private byte[] certificateDer;

    @Column(name = "fingerprint_sha256", nullable = false)
    private String fingerprintSha256;

    @Column(name = "not_after", nullable = false)
    private Instant notAfter;

    protected SpCertificate() {
    }

    public SpCertificate(ServiceProvider sp, String usage, X509Certificate certificate, String fingerprint) {
        this.id = UUID.randomUUID();
        this.serviceProvider = sp;
        this.usage = usage;
        try {
            this.certificateDer = certificate.getEncoded();
            this.notAfter = certificate.getNotAfter().toInstant();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid SP certificate", ex);
        }
        this.fingerprintSha256 = fingerprint;
    }

    public String getUsage() {
        return usage;
    }

    public X509Certificate toCertificate() {
        try {
            return (X509Certificate)
                    CertificateFactory.getInstance("X.509")
                            .generateCertificate(new java.io.ByteArrayInputStream(certificateDer));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
