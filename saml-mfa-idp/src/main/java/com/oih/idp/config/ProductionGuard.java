package com.oih.idp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionGuard implements ApplicationRunner {

    private final Environment environment;

    public ProductionGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProd()) {
            return;
        }
        String url = environment.getProperty("spring.datasource.url", "");
        if (url.contains(":h2:") || url.contains("mem:")) {
            throw new IllegalStateException("Production refuses H2 or in-memory databases.");
        }
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalStateException("Production requires PostgreSQL.");
        }
        String password = environment.getProperty("spring.datasource.password", "");
        if (password.isBlank() || "oih_dev_only".equals(password) || "password".equals(password)) {
            throw new IllegalStateException("Production refuses default datasource passwords.");
        }
        String setupToken = environment.getProperty("idp.setup.token", "");
        if (setupToken.isBlank() || "change-me".equals(setupToken)) {
            throw new IllegalStateException("Production requires a unique OIH_SETUP_TOKEN.");
        }
    }

    private boolean isProd() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}
