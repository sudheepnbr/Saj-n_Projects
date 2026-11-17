package com.oih.idp.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "idp")
public class IdpProperties {

    private String displayName = "IMT InfraMindTech";
    private String entityId = "https://idp.example.org/saml2/idp/metadata";
    private String baseUrl = "http://127.0.0.1:8080";
    private final Password password = new Password();
    private final Lockout lockout = new Lockout();
    private final Setup setup = new Setup();
    private final Mfa mfa = new Mfa();
    private final Crypto crypto = new Crypto();
    private final Saml saml = new Saml();
    private final Session session = new Session();

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

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public Saml getSaml() {
        return saml;
    }

    public Session getSession() {
        return session;
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
        private int digits = 6;
        private int periodSeconds = 30;
        private int window = 1;
        private int recoveryCodeCount = 10;
        private boolean recoveryRequiresReset = true;
        private String envelopeKey = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getDigits() {
            return digits;
        }

        public void setDigits(int digits) {
            this.digits = digits;
        }

        public int getPeriodSeconds() {
            return periodSeconds;
        }

        public void setPeriodSeconds(int periodSeconds) {
            this.periodSeconds = periodSeconds;
        }

        public int getWindow() {
            return window;
        }

        public void setWindow(int window) {
            this.window = window;
        }

        public int getRecoveryCodeCount() {
            return recoveryCodeCount;
        }

        public void setRecoveryCodeCount(int recoveryCodeCount) {
            this.recoveryCodeCount = recoveryCodeCount;
        }

        public boolean isRecoveryRequiresReset() {
            return recoveryRequiresReset;
        }

        public void setRecoveryRequiresReset(boolean recoveryRequiresReset) {
            this.recoveryRequiresReset = recoveryRequiresReset;
        }

        public String getEnvelopeKey() {
            return envelopeKey;
        }

        public void setEnvelopeKey(String envelopeKey) {
            this.envelopeKey = envelopeKey;
        }
    }

    public static class Crypto {
        /** software (in-memory non-extractable token) or pkcs11 (SunPKCS11 / SoftHSM). */
        private String token = "software";
        private String library = "/usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so";
        private String pin = "";
        private int slotListIndex = 0;
        private int keySize = 3072;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getLibrary() {
            return library;
        }

        public void setLibrary(String library) {
            this.library = library;
        }

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }

        public int getSlotListIndex() {
            return slotListIndex;
        }

        public void setSlotListIndex(int slotListIndex) {
            this.slotListIndex = slotListIndex;
        }

        public int getKeySize() {
            return keySize;
        }

        public void setKeySize(int keySize) {
            this.keySize = keySize;
        }
    }

    public static class Saml {
        private Duration assertionTtl = Duration.ofMinutes(5);
        private Duration clockSkew = Duration.ofMinutes(2);
        private Duration requestMaxAge = Duration.ofMinutes(10);
        private int maxBodyBytes = 262144;
        private boolean wantAuthnRequestsSigned = true;
        private String partialLogoutMode = "SUCCESS";

        public Duration getAssertionTtl() {
            return assertionTtl;
        }

        public void setAssertionTtl(Duration assertionTtl) {
            this.assertionTtl = assertionTtl;
        }

        public Duration getClockSkew() {
            return clockSkew;
        }

        public void setClockSkew(Duration clockSkew) {
            this.clockSkew = clockSkew;
        }

        public Duration getRequestMaxAge() {
            return requestMaxAge;
        }

        public void setRequestMaxAge(Duration requestMaxAge) {
            this.requestMaxAge = requestMaxAge;
        }

        public int getMaxBodyBytes() {
            return maxBodyBytes;
        }

        public void setMaxBodyBytes(int maxBodyBytes) {
            this.maxBodyBytes = maxBodyBytes;
        }

        public boolean isWantAuthnRequestsSigned() {
            return wantAuthnRequestsSigned;
        }

        public void setWantAuthnRequestsSigned(boolean wantAuthnRequestsSigned) {
            this.wantAuthnRequestsSigned = wantAuthnRequestsSigned;
        }

        public String getPartialLogoutMode() {
            return partialLogoutMode;
        }

        public void setPartialLogoutMode(String partialLogoutMode) {
            this.partialLogoutMode = partialLogoutMode;
        }
    }

    public static class Session {
        private Duration idle = Duration.ofMinutes(30);
        private Duration absolute = Duration.ofHours(8);
        private int maxConcurrent = 5;

        public Duration getIdle() {
            return idle;
        }

        public void setIdle(Duration idle) {
            this.idle = idle;
        }

        public Duration getAbsolute() {
            return absolute;
        }

        public void setAbsolute(Duration absolute) {
            this.absolute = absolute;
        }

        public int getMaxConcurrent() {
            return maxConcurrent;
        }

        public void setMaxConcurrent(int maxConcurrent) {
            this.maxConcurrent = maxConcurrent;
        }
    }
}
