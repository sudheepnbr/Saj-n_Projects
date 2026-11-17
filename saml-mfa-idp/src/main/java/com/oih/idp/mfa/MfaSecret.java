package com.oih.idp.mfa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mfa_secrets")
public class MfaSecret {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "secret_ciphertext", nullable = false)
    private byte[] secretCiphertext;

    @Column(nullable = false, length = 16)
    private String algorithm = "SHA1";

    @Column(nullable = false)
    private int digits = 6;

    @Column(name = "period_seconds", nullable = false)
    private int periodSeconds = 30;

    @Column(name = "last_timestep")
    private Long lastTimestep;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MfaSecret() {
    }

    public MfaSecret(UUID userId, byte[] secretCiphertext) {
        this.userId = userId;
        this.secretCiphertext = secretCiphertext;
        this.createdAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public byte[] getSecretCiphertext() {
        return secretCiphertext;
    }

    public Long getLastTimestep() {
        return lastTimestep;
    }

    public void setLastTimestep(Long lastTimestep) {
        this.lastTimestep = lastTimestep;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public boolean isConfirmed() {
        return confirmedAt != null;
    }

    public void confirm() {
        this.confirmedAt = Instant.now();
    }

    public void replaceCiphertext(byte[] secretCiphertext) {
        this.secretCiphertext = secretCiphertext;
        this.confirmedAt = null;
        this.lastTimestep = null;
        this.createdAt = Instant.now();
    }
}
