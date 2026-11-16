package com.oih.idp.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oih.idp.identity.AuthenticationService;
import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.Role;
import com.oih.idp.support.PostgresIntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class LoginEnumerationIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "CorrectHorseBattery1!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private IdentityService identity;

    @BeforeEach
    void seedUser() {
        identity.createUser("alice", "alice@example.test", "Alice", "Example", PASSWORD, Set.of(Role.USER));
    }

    @Test
    void unknownUserAndBadPasswordShowTheSameMessage() throws Exception {
        String unknown = errorMessage("nobody", "wrong-password-!!");
        String badPassword = errorMessage("alice", "wrong-password-!!");
        assertThat(unknown).isEqualTo(AuthenticationService.GENERIC_FAILURE);
        assertThat(badPassword).isEqualTo(AuthenticationService.GENERIC_FAILURE);
        assertThat(unknown).isEqualTo(badPassword);
    }

    @Test
    void disabledAccountDoesNotChangeThePublicMessage() throws Exception {
        var user = identity.listUsers().getFirst();
        identity.disable(user.getId());
        assertThat(errorMessage("alice", PASSWORD)).isEqualTo(AuthenticationService.GENERIC_FAILURE);
    }

    private String errorMessage(String username, String password) throws Exception {
        MvcResult login = mvc.perform(post("/login")
                        .param("username", username)
                        .param("password", password)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        String body = mvc.perform(get("/login").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(body).contains("class=\"flash err\"");
        if (body.contains(AuthenticationService.GENERIC_FAILURE)) {
            return AuthenticationService.GENERIC_FAILURE;
        }
        if (body.contains(AuthenticationService.LOCKED)) {
            return AuthenticationService.LOCKED;
        }
        if (body.contains(AuthenticationService.DISABLED)) {
            return AuthenticationService.DISABLED;
        }
        throw new AssertionError("No known login error in body: " + body);
    }
}
