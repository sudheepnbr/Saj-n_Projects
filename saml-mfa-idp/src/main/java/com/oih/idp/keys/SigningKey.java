package com.oih.idp.keys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "signing_keys")
public class SigningKey {

    public enum Status {
        NEXT,
        ACTIVE,
        RETIRING,
        DESTROYED
    }

    @Id
    private UUID id;

    @Column(name = "cka_id", nullable = false, unique = true, length = 64)
    private String ckaId;

    @Column(nullable = false, length = 128)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "certificate_der", nullable = false)
    private byte[] certificateDer;

    @Column(name = "fingerprint_sha256", nullable = false, length = 64)
    private String fingerprintSha256;

    @Column(name = "not_before", nullable = false)
    private Instant notBefore;

    @Column(name = "not_after", nullable = false)
    private Instant notAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SigningKey() {
    }

    public SigningKey(
            String ckaId,
            String label,
            Status status,
            byte[] certificateDer,
            String fingerprintSha256,
            Instant notBefore,
            Instant notAfter) {
        this.id = UUID.randomUUID();
        this.ckaId = ckaId;
        this.label = label;
        this.status = status;
        this.certificateDer = certificateDer;
        this.fingerprintSha256 = fingerprintSha256;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCkaId() {
        return ckaId;
    }

    public String getLabel() {
        return label;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public byte[] getCertificateDer() {
        return certificateDer;
    }

    public String getFingerprintSha256() {
        return fingerprintSha256;
    }

    public Instant getNotAfter() {
        return notAfter;
    }
}
