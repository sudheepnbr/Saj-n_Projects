package com.oih.idp.policy;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.identity.domain.AuthSourceThrottle;
import com.oih.idp.identity.domain.User;
import com.oih.idp.identity.repo.AuthSourceThrottleRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LockoutService {

    private final IdpProperties.Lockout settings;
    private final AuthSourceThrottleRepository throttleRepository;

    public LockoutService(IdpProperties properties, AuthSourceThrottleRepository throttleRepository) {
        this.settings = properties.getLockout();
        this.throttleRepository = throttleRepository;
    }

    public boolean isUserLocked(User user, Instant now) {
        return user.isLocked(now);
    }

    public Instant lockUntilForFailures(int failures) {
        Instant now = Instant.now();
        if (failures >= settings.getThirdThreshold()) {
            return now.plus(settings.getThirdLockDuration());
        }
        if (failures >= settings.getSecondThreshold()) {
            return now.plus(settings.getSecondLockDuration());
        }
        if (failures >= settings.getFailureThreshold()) {
            return now.plus(settings.getLockDuration());
        }
        return null;
    }

    @Transactional
    public boolean isSourceLocked(String source, Instant now) {
        return throttleRepository.findById(source)
                .map(row -> row.isLocked(now))
                .orElse(false);
    }

    @Transactional
    public void recordSourceFailure(String source, Instant now) {
        AuthSourceThrottle row = throttleRepository.findById(source)
                .orElseGet(() -> new AuthSourceThrottle(source, now));
        Duration window = settings.getIpWindow();
        if (row.getWindowStartedAt().plus(window).isBefore(now)) {
            row.resetWindow(now);
        }
        Instant lockUntil = null;
        if (row.getFailures() + 1 >= settings.getIpFailureThreshold()) {
            lockUntil = now.plus(window);
        }
        row.increment(lockUntil);
        throttleRepository.save(row);
    }

    public int failureThreshold() {
        return settings.getFailureThreshold();
    }
}
