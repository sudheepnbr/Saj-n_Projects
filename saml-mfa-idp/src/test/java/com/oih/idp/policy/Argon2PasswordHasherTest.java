package com.oih.idp.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.oih.idp.config.IdpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Argon2PasswordHasherTest {

    private Argon2PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        IdpProperties properties = new IdpProperties();
        properties.getPassword().setArgon2MemoryKib(1024);
        properties.getPassword().setArgon2Iterations(1);
        hasher = new Argon2PasswordHasher(properties);
    }

    @Test
    void hashesAndVerifiesArgon2id() {
        String encoded = hasher.hash("CorrectHorseBattery1!");
        assertThat(encoded).startsWith("$argon2id$v=19$");
        assertThat(hasher.matches("CorrectHorseBattery1!", encoded)).isTrue();
        assertThat(hasher.matches("wrong-password-1!", encoded)).isFalse();
    }

    @Test
    void dummyMatchAlwaysFalseButRuns() {
        assertThat(hasher.matchesDummy("anything-at-all")).isFalse();
    }
}
