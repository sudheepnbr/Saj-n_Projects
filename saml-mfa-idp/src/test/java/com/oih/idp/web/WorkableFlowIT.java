package com.oih.idp.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oih.idp.identity.SetupService;
import com.oih.idp.identity.domain.AdminBootstrap;
import com.oih.idp.identity.repo.AdminBootstrapRepository;
import com.oih.idp.policy.Argon2PasswordHasher;
import com.oih.idp.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class WorkableFlowIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "CorrectHorseBattery1!";
    private static final String SP = "https://app.example.test/metadata";
    private static final String ACS = "https://app.example.test/acs";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SetupService setup;

    @Autowired
    private AdminBootstrapRepository bootstrap;

    @Autowired
    private Argon2PasswordHasher hasher;

    @BeforeEach
    void mintSetupToken() {
        setup.ensureBootstrapToken();
    }

    @Test
    void leftoverBootstrapHashIsReplacedByConfiguredToken() {
        bootstrap.findFirstByConsumedAtIsNullOrderByCreatedAtAsc().ifPresent(row -> {
            row.consume();
            bootstrap.save(row);
        });
        bootstrap.save(new AdminBootstrap(hasher.hash("stale-random-token")));
        setup.ensureBootstrapToken();
        setup.createFirstAdmin("test-setup-token", "resync", "resync@example.test", "Re", "Sync", PASSWORD);
        assertThat(setup.setupRequired()).isFalse();
    }

    @Test
    void firstRunSetupLoginAdminSpAndIdpInitiatedSso() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/setup"));
        mvc.perform(get("/login")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/setup"));
        mvc.perform(get("/setup")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Create the first administrator")));

        mvc.perform(post("/setup")
                        .with(csrf())
                        .param("token", "wrong-token")
                        .param("username", "bootstrap")
                        .param("email", "admin@example.test")
                        .param("firstName", "Boot")
                        .param("lastName", "Strap")
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("invalid")));

        mvc.perform(post("/setup")
                        .with(csrf())
                        .param("token", "test-setup-token")
                        .param("username", "bootstrap")
                        .param("email", "admin@example.test")
                        .param("firstName", "Boot")
                        .param("lastName", "Strap")
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?setup"));

        mvc.perform(get("/setup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        MvcResult login = mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "bootstrap")
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        jakarta.servlet.http.Cookie sessionCookie = login.getResponse().getCookie("IMTIDPSESS");
        if (sessionCookie != null) {
            mvc.perform(get("/account").cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("bootstrap")));
        }

        mvc.perform(get("/account").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bootstrap")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Admin")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Enroll authenticator")));
        mvc.perform(get("/setup").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
        mvc.perform(get("/admin/keys").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ACTIVE")));
        mvc.perform(get("/saml2/idp/metadata"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("IDPSSODescriptor")));

        mvc.perform(post("/admin/sp")
                        .session(session)
                        .with(csrf())
                        .param("entityId", SP)
                        .param("displayName", "Demo App")
                        .param("acs", ACS)
                        .param("slo", "https://app.example.test/slo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/sp"));

        mvc.perform(get("/account").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Demo App")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/saml2/idp/initiate")));

        MvcResult sso = mvc.perform(get("/saml2/idp/initiate").param("sp", SP).session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(sso.getResponse().getContentAsString()).contains("SAMLResponse");

        MvcResult firstEnroll = mvc.perform(get("/mfa/enroll").session(session)).andExpect(status().isOk()).andReturn();
        String firstQr = otpauth(firstEnroll.getResponse().getContentAsString());
        MvcResult secondEnroll = mvc.perform(get("/mfa/enroll").session(session)).andExpect(status().isOk()).andReturn();
        assertThat(otpauth(secondEnroll.getResponse().getContentAsString())).isEqualTo(firstQr);
    }

    private static String otpauth(String html) {
        int start = html.indexOf("otpauth://");
        assertThat(start).isGreaterThanOrEqualTo(0);
        int end = html.indexOf("</code>", start);
        return html.substring(start, end);
    }
}
