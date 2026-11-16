package com.oih.idp.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oih.idp.identity.AuthenticationService;
import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.Role;
import com.oih.idp.identity.domain.User;
import com.oih.idp.support.PostgresIntegrationTest;
import com.oih.idp.web.security.IdpUserPrincipal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AdminLifecycleIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "CorrectHorseBattery1!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private IdentityService identity;

    @Autowired
    private AuthenticationService authentication;

    private IdpUserPrincipal adminPrincipal;

    @BeforeEach
    void seedAdmin() {
        User admin = identity.createUser(
                "rootadmin", "admin@example.test", "Root", "Admin", PASSWORD, Set.of(Role.ADMIN, Role.USER));
        adminPrincipal = new IdpUserPrincipal(admin);
    }

    @Test
    void adminCanCreateDisableAndUnlockUsers() throws Exception {
        mvc.perform(post("/admin/users")
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .param("username", "carol")
                        .param("email", "carol@example.test")
                        .param("firstName", "Carol")
                        .param("lastName", "Example")
                        .param("password", PASSWORD)
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        User carol = identity.listUsers().stream()
                .filter(user -> "carol".equals(user.getUsername()))
                .findFirst()
                .orElseThrow();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.20");
        assertThat(authentication.authenticate("carol", PASSWORD, request).success()).isTrue();

        mvc.perform(post("/admin/users/{id}/disable", carol.getId())
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(authentication.authenticate("carol", PASSWORD, request).success()).isFalse();

        mvc.perform(post("/admin/users/{id}/enable", carol.getId())
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        for (int i = 0; i < 5; i++) {
            authentication.authenticate("carol", "wrong-password-!!", request);
        }
        assertThat(authentication.authenticate("carol", PASSWORD, request).failureCategory()).isEqualTo("LOCKED");

        mvc.perform(post("/admin/users/{id}/unlock", carol.getId())
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(authentication.authenticate("carol", PASSWORD, request).success()).isTrue();
    }
}
