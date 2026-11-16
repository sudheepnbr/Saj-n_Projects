package com.oih.idp.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "auth_source_throttle")
public class AuthSourceThrottle {

    @Id
    @Column(length = 64)
    private String source;

    @Column(nullable = false)
    private int failures;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected AuthSourceThrottle() {
    }

    public AuthSourceThrottle(String source, Instant now) {
        this.source = source;
        this.failures = 0;
        this.windowStartedAt = now;
    }

    public String getSource() {
        return source;
    }

    public int getFailures() {
        return failures;
    }

    public Instant getWindowStartedAt() {
        return windowStartedAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void resetWindow(Instant now) {
        this.failures = 0;
        this.windowStartedAt = now;
        this.lockedUntil = null;
    }

    public void increment(Instant lockUntil) {
        this.failures += 1;
        this.lockedUntil = lockUntil;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
