package com.oih.idp.crypto;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class HsmHealthIndicator implements HealthIndicator {

    private final SigningService signingService;

    public HsmHealthIndicator(SigningService signingService) {
        this.signingService = signingService;
    }

    @Override
    public Health health() {
        try {
            byte[] nonce = new byte[32];
            new SecureRandom().nextBytes(nonce);
            byte[] signature = signingService.sign(nonce);
            X509Certificate certificate = signingService.activeCertificate();
            if (!signingService.verify(nonce, signature, certificate)) {
                return Health.down().withDetail("hsm", "sign-self-test mismatch").build();
            }
            return Health.up().withDetail("hsm", "sign-self-test ok").build();
        } catch (Exception ex) {
            return Health.down().withDetail("hsm", ex.getMessage()).build();
        }
    }
}
