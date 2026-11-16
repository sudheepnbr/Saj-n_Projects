package com.oih.idp.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.oih.idp.config.IdpProperties;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LockoutPolicyTest {

    @Test
    void lockDurationEscalatesWithFailureCount() {
        IdpProperties properties = new IdpProperties();
        LockoutService lockout = new LockoutService(properties, null);
        Instant first = lockout.lockUntilForFailures(5);
        Instant second = lockout.lockUntilForFailures(8);
        Instant third = lockout.lockUntilForFailures(12);
        Instant none = lockout.lockUntilForFailures(2);

        assertThat(none).isNull();
        assertThat(Duration.between(Instant.now().minusSeconds(2), first))
                .isCloseTo(properties.getLockout().getLockDuration(), Duration.ofSeconds(5));
        assertThat(second).isAfter(first);
        assertThat(third).isAfter(second);
    }
}
