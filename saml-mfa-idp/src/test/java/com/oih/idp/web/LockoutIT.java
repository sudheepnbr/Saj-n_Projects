package com.oih.idp.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.oih.idp.identity.AuthenticationService;
import com.oih.idp.identity.AuthenticationService.AuthResult;
import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.Role;
import com.oih.idp.identity.domain.User;
import com.oih.idp.identity.repo.UserRepository;
import com.oih.idp.support.PostgresIntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

class LockoutIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "CorrectHorseBattery1!";

    @Autowired
    private IdentityService identity;

    @Autowired
    private AuthenticationService authentication;

    @Autowired
    private UserRepository users;

    @BeforeEach
    void seedUser() {
        identity.createUser("bob", "bob@example.test", "Bob", "Example", PASSWORD, Set.of(Role.USER));
    }

    @Test
    void lockoutPersistsInPostgresAndBlocksLaterAttempts() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");

        AuthResult lastFailure = null;
        for (int i = 0; i < 5; i++) {
            lastFailure = authentication.authenticate("bob", "not-the-password!!", request);
            assertThat(lastFailure.success()).isFalse();
        }
        assertThat(lastFailure.message()).isEqualTo(AuthenticationService.LOCKED);

        User stored = users.findByUsernameNormalized("bob").orElseThrow();
        assertThat(stored.getLockedUntil()).isNotNull();
        assertThat(stored.getFailedLogins()).isGreaterThanOrEqualTo(5);

        InstantLockFromDb lock = jdbcTemplate.queryForObject(
                "select locked_until from users where username_normalized = 'bob'",
                (rs, rowNum) -> new InstantLockFromDb(rs.getTimestamp("locked_until") != null));
        assertThat(lock.present()).isTrue();

        AuthResult afterPersist = authentication.authenticate("bob", PASSWORD, request);
        assertThat(afterPersist.success()).isFalse();
        assertThat(afterPersist.message()).isEqualTo(AuthenticationService.LOCKED);
        assertThat(afterPersist.failureCategory()).isEqualTo("LOCKED");
    }

    private record InstantLockFromDb(boolean present) {
    }
}
