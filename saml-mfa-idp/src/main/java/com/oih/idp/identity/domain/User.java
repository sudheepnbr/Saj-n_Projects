package com.oih.idp.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "username_normalized", nullable = false, unique = true, length = 64)
    private String usernameNormalized;

    @Column(nullable = false, length = 256)
    private String email;

    @Column(name = "email_normalized", nullable = false, unique = true, length = 256)
    private String emailNormalized;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "failed_logins", nullable = false)
    private int failedLogins = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_failed_login_at")
    private Instant lastFailedLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    @Version
    private int version;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected User() {
    }

    public static User create(
            String username,
            String email,
            String firstName,
            String lastName,
            String passwordHash) {
        Instant now = Instant.now();
        User user = new User();
        user.id = UUID.randomUUID();
        user.setUsername(username);
        user.setEmail(email);
        user.firstName = firstName;
        user.lastName = lastName;
        user.passwordHash = passwordHash;
        user.passwordChangedAt = now;
        user.createdAt = now;
        user.modifiedAt = now;
        return user;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username.trim();
        this.usernameNormalized = normalize(username);
        touch();
    }

    public String getUsernameNormalized() {
        return usernameNormalized;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email.trim();
        this.emailNormalized = normalize(email);
        touch();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void replacePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = Instant.now();
        touch();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        touch();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        touch();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        touch();
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public int getFailedLogins() {
        return failedLogins;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailedLogin(Instant now, Instant lockUntil) {
        this.failedLogins += 1;
        this.lastFailedLoginAt = now;
        this.lockedUntil = lockUntil;
        touch();
    }

    public void resetFailures() {
        this.failedLogins = 0;
        this.lockedUntil = null;
        touch();
    }

    public void unlock() {
        this.failedLogins = 0;
        this.lockedUntil = null;
        touch();
    }

    public void markLoginSuccess(Instant now) {
        this.lastLoginAt = now;
        this.failedLogins = 0;
        this.lockedUntil = null;
        touch();
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(role -> role.getName().equals(roleName));
    }

    public static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void touch() {
        this.modifiedAt = Instant.now();
    }
}
