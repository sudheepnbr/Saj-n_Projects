package com.oih.idp.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.oih.idp.config.IdpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private PasswordPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new PasswordPolicy(new IdpProperties());
    }

    @Test
    void rejectsShortPassword() {
        assertThat(policy.validate("Short1!")).isNotEmpty();
    }

    @Test
    void rejectsTooFewCharacterClasses() {
        assertThat(policy.validate("alllowercase1")).isNotEmpty();
    }

    @Test
    void acceptsStrongPassword() {
        assertThat(policy.validate("CorrectHorseBattery1!")).isEmpty();
    }
}
