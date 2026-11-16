package com.oih.idp.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "idp")
public class IdpProperties {

    private String displayName = "OIH Local Identity Provider";
    private final Password password = new Password();
    private final Lockout lockout = new Lockout();
    private final Setup setup = new Setup();
    private final Mfa mfa = new Mfa();

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Password getPassword() {
        return password;
    }

    public Lockout getLockout() {
        return lockout;
    }

    public Setup getSetup() {
        return setup;
    }

    public Mfa getMfa() {
        return mfa;
    }

    public static class Password {
        private int minLength = 12;
        private int maxLength = 128;
        private int requiredCharacterClasses = 3;
        private int historySize = 5;
        private int argon2SaltLength = 16;
        private int argon2HashLength = 32;
        private int argon2Parallelism = 1;
        private int argon2MemoryKib = 16384;
        private int argon2Iterations = 2;

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public int getRequiredCharacterClasses() {
            return requiredCharacterClasses;
        }

        public void setRequiredCharacterClasses(int requiredCharacterClasses) {
            this.requiredCharacterClasses = requiredCharacterClasses;
        }

        public int getHistorySize() {
            return historySize;
        }

        public void setHistorySize(int historySize) {
            this.historySize = historySize;
        }

        public int getArgon2SaltLength() {
            return argon2SaltLength;
        }

        public void setArgon2SaltLength(int argon2SaltLength) {
            this.argon2SaltLength = argon2SaltLength;
        }

        public int getArgon2HashLength() {
            return argon2HashLength;
        }

        public void setArgon2HashLength(int argon2HashLength) {
            this.argon2HashLength = argon2HashLength;
        }

        public int getArgon2Parallelism() {
            return argon2Parallelism;
        }

        public void setArgon2Parallelism(int argon2Parallelism) {
            this.argon2Parallelism = argon2Parallelism;
        }

        public int getArgon2MemoryKib() {
            return argon2MemoryKib;
        }

        public void setArgon2MemoryKib(int argon2MemoryKib) {
            this.argon2MemoryKib = argon2MemoryKib;
        }

        public int getArgon2Iterations() {
            return argon2Iterations;
        }

        public void setArgon2Iterations(int argon2Iterations) {
            this.argon2Iterations = argon2Iterations;
        }
    }

    public static class Lockout {
        private int failureThreshold = 5;
        private Duration lockDuration = Duration.ofMinutes(15);
        private int secondThreshold = 8;
        private Duration secondLockDuration = Duration.ofHours(1);
        private int thirdThreshold = 12;
        private Duration thirdLockDuration = Duration.ofHours(24);
        private Duration ipWindow = Duration.ofMinutes(15);
        private int ipFailureThreshold = 30;

        public int getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        public Duration getLockDuration() {
            return lockDuration;
        }

        public void setLockDuration(Duration lockDuration) {
            this.lockDuration = lockDuration;
        }

        public int getSecondThreshold() {
            return secondThreshold;
        }

        public void setSecondThreshold(int secondThreshold) {
            this.secondThreshold = secondThreshold;
        }

        public Duration getSecondLockDuration() {
            return secondLockDuration;
        }

        public void setSecondLockDuration(Duration secondLockDuration) {
            this.secondLockDuration = secondLockDuration;
        }

        public int getThirdThreshold() {
            return thirdThreshold;
        }

        public void setThirdThreshold(int thirdThreshold) {
            this.thirdThreshold = thirdThreshold;
        }

        public Duration getThirdLockDuration() {
            return thirdLockDuration;
        }

        public void setThirdLockDuration(Duration thirdLockDuration) {
            this.thirdLockDuration = thirdLockDuration;
        }

        public Duration getIpWindow() {
            return ipWindow;
        }

        public void setIpWindow(Duration ipWindow) {
            this.ipWindow = ipWindow;
        }

        public int getIpFailureThreshold() {
            return ipFailureThreshold;
        }

        public void setIpFailureThreshold(int ipFailureThreshold) {
            this.ipFailureThreshold = ipFailureThreshold;
        }
    }

    public static class Setup {
        private String token = "";

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class Mfa {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
