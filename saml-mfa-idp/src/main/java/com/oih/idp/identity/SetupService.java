package com.oih.idp.identity;

import com.oih.idp.audit.AuditService;
import com.oih.idp.config.IdpProperties;
import com.oih.idp.identity.domain.AdminBootstrap;
import com.oih.idp.identity.domain.Role;
import com.oih.idp.identity.repo.AdminBootstrapRepository;
import com.oih.idp.identity.repo.UserRepository;
import com.oih.idp.policy.Argon2PasswordHasher;
import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupService {

    private static final Logger log = LoggerFactory.getLogger(SetupService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final AdminBootstrapRepository bootstrap;
    private final IdentityService identity;
    private final Argon2PasswordHasher hasher;
    private final IdpProperties properties;
    private final AuditService audit;
    private final Environment environment;

    public SetupService(
            UserRepository users,
            AdminBootstrapRepository bootstrap,
            IdentityService identity,
            Argon2PasswordHasher hasher,
            IdpProperties properties,
            AuditService audit,
            Environment environment) {
        this.users = users;
        this.bootstrap = bootstrap;
        this.identity = identity;
        this.hasher = hasher;
        this.properties = properties;
        this.audit = audit;
        this.environment = environment;
    }

    @PostConstruct
    public void ensureBootstrapToken() {
        if (users.count() > 0) {
            return;
        }
        if (bootstrap.existsByConsumedAtIsNull()) {
            return;
        }
        String token = properties.getSetup().getToken();
        if (token == null || token.isBlank()) {
            byte[] bytes = new byte[24];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            if (!environment.acceptsProfiles(Profiles.of("prod", "production"))) {
                log.warn("First-run setup token (dev only): {}", token);
            } else {
                log.warn("OIH_SETUP_TOKEN was empty; a token was generated but will not be logged in production.");
            }
        }
        bootstrap.save(new AdminBootstrap(hasher.hash(token)));
    }

    public boolean setupRequired() {
        return users.count() == 0;
    }

    @Transactional
    public void createFirstAdmin(
            String token,
            String username,
            String email,
            String firstName,
            String lastName,
            String password) {
        if (!setupRequired()) {
            throw new IdentityException("Setup has already been completed.");
        }
        AdminBootstrap pending = bootstrap
                .findFirstByConsumedAtIsNullOrderByCreatedAtAsc()
                .orElseThrow(() -> new IdentityException("Setup token is not available."));
        if (!hasher.matches(token, pending.getTokenHash())) {
            audit.record("SETUP", "FAILURE", null, null, "BAD_TOKEN");
            throw new IdentityException("Setup token is invalid.");
        }
        identity.createUser(username, email, firstName, lastName, password, Set.of(Role.ADMIN, Role.USER));
        pending.consume();
        bootstrap.save(pending);
        audit.record("SETUP", "SUCCESS", null, null, null);
    }
}
