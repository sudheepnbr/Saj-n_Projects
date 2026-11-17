package com.oih.idp.mfa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oih.idp.identity.IdentityException;
import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.Role;
import com.oih.idp.identity.domain.User;
import com.oih.idp.support.PostgresIntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "idp.mfa.enabled=true")
class MfaIT extends PostgresIntegrationTest {

    @Autowired
    private IdentityService identity;

    @Autowired
    private MfaService mfa;

    @Autowired
    private TotpService totp;

    @Test
    void enrollmentRequiresConfirmAndRejectsReplayAndRecoveryReuse() {
        User user = identity.createUser(
                "mfa-user", "mfa@example.test", "Mfa", "User", "CorrectHorseBattery1!", Set.of(Role.USER));
        var enrollment = mfa.startEnrollment(user);
        assertThat(mfa.isEnrolled(user.getId())).isFalse();
        assertThat(user.isMfaEnabled()).isFalse();

        String code = totp.otp(enrollment.secret(), totp.currentTimestep());
        mfa.confirmEnrollment(identity.requireUser(user.getId()), code);
        assertThat(mfa.isEnrolled(user.getId())).isTrue();

        assertThat(mfa.verifyTotp(identity.requireUser(user.getId()), code)).isFalse();

        User reloaded = identity.requireUser(user.getId());
        String recovery = enrollment.recoveryCodes().getFirst();
        assertThat(mfa.consumeRecoveryCode(reloaded, recovery)).isTrue();
        assertThat(mfa.consumeRecoveryCode(identity.requireUser(user.getId()), recovery)).isFalse();
    }

    @Test
    void confirmFailsOnBadCode() {
        User user = identity.createUser(
                "mfa-bad", "mfabad@example.test", "Mfa", "Bad", "CorrectHorseBattery1!", Set.of(Role.USER));
        mfa.startEnrollment(user);
        assertThatThrownBy(() -> mfa.confirmEnrollment(user, "000000")).isInstanceOf(IdentityException.class);
        assertThat(mfa.isEnrolled(user.getId())).isFalse();
    }
}
