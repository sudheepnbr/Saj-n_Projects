package com.oih.idp.identity;

import com.oih.idp.audit.AuditService;
import com.oih.idp.identity.domain.User;
import com.oih.idp.identity.repo.UserRepository;
import com.oih.idp.policy.Argon2PasswordHasher;
import com.oih.idp.policy.LockoutService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    public static final String GENERIC_FAILURE = "Invalid username or password.";
    public static final String LOCKED = "This account is temporarily locked. Try again later.";
    public static final String SOURCE_LOCKED = "Too many failed attempts from this network. Try again later.";
    public static final String DISABLED = "This account is disabled.";

    private final UserRepository users;
    private final Argon2PasswordHasher hasher;
    private final LockoutService lockout;
    private final AuditService audit;

    public AuthenticationService(
            UserRepository users,
            Argon2PasswordHasher hasher,
            LockoutService lockout,
            AuditService audit) {
        this.users = users;
        this.hasher = hasher;
        this.lockout = lockout;
        this.audit = audit;
    }

    @Transactional
    public AuthResult authenticate(String username, String password, HttpServletRequest request) {
        Instant now = Instant.now();
        String source = request == null ? "unknown" : request.getRemoteAddr();

        if (lockout.isSourceLocked(source, now)) {
            audit.record("LOGIN", "FAILURE", null, request, "SOURCE_LOCKED");
            return AuthResult.failure(SOURCE_LOCKED, "SOURCE_LOCKED");
        }

        User user = users.findByUsernameNormalized(User.normalize(username)).orElse(null);
        if (user == null) {
            hasher.matchesDummy(password);
            lockout.recordSourceFailure(source, now);
            audit.record("LOGIN", "FAILURE", null, request, "UNKNOWN_USER");
            return AuthResult.failure(GENERIC_FAILURE, "UNKNOWN_USER");
        }

        if (lockout.isUserLocked(user, now)) {
            hasher.matchesDummy(password);
            audit.record("LOGIN", "FAILURE", user.getId(), request, "LOCKED");
            return AuthResult.failure(LOCKED, "LOCKED");
        }

        if (!user.isEnabled()) {
            hasher.matchesDummy(password);
            audit.record("LOGIN", "FAILURE", user.getId(), request, "DISABLED");
            return AuthResult.failure(GENERIC_FAILURE, "DISABLED");
        }

        boolean matches = hasher.matches(password, user.getPasswordHash());
        if (!matches) {
            Instant lockUntil = lockout.lockUntilForFailures(user.getFailedLogins() + 1);
            user.recordFailedLogin(now, lockUntil);
            users.save(user);
            lockout.recordSourceFailure(source, now);
            String category = lockUntil != null ? "LOCKED" : "BAD_PASSWORD";
            audit.record("LOGIN", "FAILURE", user.getId(), request, category);
            return AuthResult.failure(lockUntil != null ? LOCKED : GENERIC_FAILURE, category);
        }

        user.markLoginSuccess(now);
        users.save(user);
        audit.record("LOGIN", "SUCCESS", user.getId(), request, null);
        return AuthResult.success(user);
    }

    public record AuthResult(boolean success, User user, String message, String failureCategory) {
        static AuthResult success(User user) {
            return new AuthResult(true, user, null, null);
        }

        static AuthResult failure(String message, String category) {
            return new AuthResult(false, null, message, category);
        }
    }
}
