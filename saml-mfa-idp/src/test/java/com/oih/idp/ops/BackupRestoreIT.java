package com.oih.idp.ops;

import static org.assertj.core.api.Assertions.assertThat;

import com.oih.idp.crypto.HsmHealthIndicator;
import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.Role;
import com.oih.idp.identity.domain.User;
import com.oih.idp.support.PostgresIntegrationTest;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Status;

class BackupRestoreIT extends PostgresIntegrationTest {

    @Autowired
    private BackupService backup;

    @Autowired
    private IdentityService identity;

    @Autowired
    private HsmHealthIndicator hsmHealth;

    @Test
    void restoreDrillRestoresEnabledFlag() {
        User user = identity.createUser(
                "backup", "backup@example.test", "Back", "Up", "CorrectHorseBattery1!", Set.of(Role.USER));
        Map<String, Object> snap = backup.snapshotIdentity();
        identity.disable(user.getId());
        assertThat(identity.requireUser(user.getId()).isEnabled()).isFalse();
        backup.restoreUsersEnabled(snap);
        assertThat(identity.requireUser(user.getId()).isEnabled()).isTrue();
    }

    @Test
    void readinessIncludesHsmSelfTest() {
        assertThat(hsmHealth.health().getStatus()).isEqualTo(Status.UP);
    }
}
