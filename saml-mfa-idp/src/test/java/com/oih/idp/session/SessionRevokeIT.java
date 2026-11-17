package com.oih.idp.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.Role;
import com.oih.idp.identity.domain.User;
import com.oih.idp.support.PostgresIntegrationTest;
import com.oih.idp.web.security.IdpUserPrincipal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

class SessionRevokeIT extends PostgresIntegrationTest {

    @Autowired
    private SsoSessionService sso;

    @Autowired
    private IdentityService identity;

    @Test
    void revokeRemovesAuthoritativeSession() {
        User user = identity.createUser(
                "sess", "sess@example.test", "Sess", "Ion", "CorrectHorseBattery1!", Set.of(Role.USER));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new org.springframework.mock.web.MockHttpSession());
        IdpSsoSession session = sso.establish(new IdpUserPrincipal(user), request);
        assertThat(sso.current(request)).isNotNull();
        sso.revoke(session.getId());
        assertThat(sso.current(request)).isNull();
    }
}
