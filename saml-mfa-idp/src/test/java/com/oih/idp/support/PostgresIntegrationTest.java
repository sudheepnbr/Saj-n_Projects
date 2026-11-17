package com.oih.idp.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public abstract class PostgresIntegrationTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanIdentityTables() {
        jdbcTemplate.execute(
                """
                TRUNCATE password_history, user_roles, users, auth_source_throttle,
                         audit_events, admin_bootstrap, mfa_secrets, recovery_codes,
                         idp_sessions, sp_sessions, logout_correlation, replay_cache,
                         persistent_nameids, attribute_mappings, sp_certificates, sp_acs,
                         sp_slo, service_providers, SPRING_SESSION_ATTRIBUTES, SPRING_SESSION
                         RESTART IDENTITY CASCADE
                """);
    }
}
