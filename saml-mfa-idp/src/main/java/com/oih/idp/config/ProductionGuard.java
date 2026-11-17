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
        if (!"pkcs11".equals(environment.getProperty("idp.crypto.token", ""))) {
            throw new IllegalStateException("Production requires PKCS#11 token mode.");
        }
        if (environment.getProperty("idp.crypto.pin", "").isBlank()) {
            throw new IllegalStateException("Production requires OIH_HSM_PIN.");
        }
        if (environment.getProperty("idp.mfa.envelope-key", "").isBlank()) {
            throw new IllegalStateException("Production requires OIH_TOTP_WRAP_KEY.");
        }
        String entityId = environment.getProperty("idp.entity-id", "");
        String baseUrl = environment.getProperty("idp.base-url", "");
        if (!entityId.startsWith("https://") || !baseUrl.startsWith("https://")) {
            throw new IllegalStateException("Production requires HTTPS entity ID and base URL.");
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
