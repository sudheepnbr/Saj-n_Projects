package com.oih.idp.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ProductionGuardTest {

    @Test
    void productionRejectsH2() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("spring.datasource.url", "jdbc:h2:mem:idp");
        env.setProperty("spring.datasource.password", "secret-enough");
        env.setProperty("idp.setup.token", "unique-setup-token");
        ProductionGuard guard = new ProductionGuard(env);
        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("H2");
    }

    @Test
    void productionRejectsDefaultPassword() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("spring.datasource.url", "jdbc:postgresql://db/oih");
        env.setProperty("spring.datasource.password", "oih_dev_only");
        env.setProperty("idp.setup.token", "unique-setup-token");
        ProductionGuard guard = new ProductionGuard(env);
        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("password");
    }
}
