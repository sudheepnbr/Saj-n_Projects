package com.oih.idp.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idp_sessions")
public class IdpSsoSession {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_index", nullable = false, unique = true, length = 64)
    private String sessionIndex;

    @Column(name = "http_session_id", length = 64)
    private String httpSessionId;

    @Column(name = "auth_state", nullable = false, length = 32)
    private String authState;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "absolute_expires_at", nullable = false)
    private Instant absoluteExpiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected IdpSsoSession() {
    }

    public static IdpSsoSession open(UUID userId, String httpSessionId, Instant absoluteExpiry) {
        Instant now = Instant.now();
        IdpSsoSession session = new IdpSsoSession();
        session.id = UUID.randomUUID();
        session.userId = userId;
        session.sessionIndex = UUID.randomUUID().toString();
        session.httpSessionId = httpSessionId;
        session.authState = "FULL";
        session.createdAt = now;
        session.lastSeenAt = now;
        session.absoluteExpiresAt = absoluteExpiry;
        return session;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSessionIndex() {
        return sessionIndex;
    }

    public String getHttpSessionId() {
        return httpSessionId;
    }

    public void setHttpSessionId(String httpSessionId) {
        this.httpSessionId = httpSessionId;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public boolean isActive() {
        return revokedAt == null && Instant.now().isBefore(absoluteExpiresAt);
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    public void touch() {
        this.lastSeenAt = Instant.now();
    }
}
