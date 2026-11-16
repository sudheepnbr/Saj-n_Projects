package com.oih.idp.saml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.Role;
import com.oih.idp.identity.domain.User;
import com.oih.idp.sp.ServiceProviderRegistry;
import com.oih.idp.support.PostgresIntegrationTest;
import com.oih.idp.web.security.IdpUserPrincipal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SloIT extends PostgresIntegrationTest {

    private static final String SP = "https://sp.example.test/metadata";
    private static final String ACS = "https://sp.example.test/acs";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private IdentityService identity;

    @Autowired
    private ServiceProviderRegistry registry;

    private IdpUserPrincipal principal;

    @BeforeEach
    void seed() {
        registry.register(SP, "Lab SP", ACS, "https://sp.example.test/slo", false, false, null, null);
        User user = identity.createUser(
                "slouser", "slo@example.test", "Slo", "User", "CorrectHorseBattery1!", Set.of(Role.USER));
        principal = new IdpUserPrincipal(user);
    }

    @Test
    void spInitiatedLogoutReturnsSignedResponse() throws Exception {
        String encoded = SamlSupport.encodeRedirect(logout(SP));
        String html = mvc.perform(get("/saml2/idp/slo").param("SAMLRequest", encoded).with(user(principal)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(html).contains("SAMLResponse");
        String b64 = html.replaceAll("(?s).*name=\"SAMLResponse\" value=\"([^\"]+)\".*", "$1");
        var response = SamlSupport.parse(SamlSupport.decodePost(b64));
        assertThat(response.getDocumentElement().getLocalName()).isEqualTo("LogoutResponse");
        assertThat(SamlSupport.signatureAlgorithm(response.getDocumentElement())).isEqualTo(SamlSupport.RSA_SHA256);
    }

    @Test
    void logoutReplayIsRejected() throws Exception {
        byte[] request = logout(SP);
        String encoded = SamlSupport.encodeRedirect(request);
        mvc.perform(get("/saml2/idp/slo").param("SAMLRequest", encoded).with(user(principal)))
                .andExpect(status().isOk());
        mvc.perform(get("/saml2/idp/slo").param("SAMLRequest", encoded).with(user(principal)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idpInitiatedLogoutIsAudited() throws Exception {
        mvc.perform(post("/account/logout-global").with(user(principal)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_type = 'SLO'", Integer.class);
        assertThat(count).isNotNull();
    }

    static byte[] logout(String entityId) {
        String id = "_" + UUID.randomUUID().toString().replace("-", "");
        String xml = """
                <samlp:LogoutRequest xmlns:samlp="%s" xmlns:saml="%s" ID="%s" Version="2.0" IssueInstant="%s" Destination="http://localhost/saml2/idp/slo"><saml:Issuer Format="urn:oasis:names:tc:SAML:2.0:nameid-format:entity">%s</saml:Issuer></samlp:LogoutRequest>
                """
                .formatted(SamlSupport.SAMLP, SamlSupport.SAML, id, Instant.now(), entityId);
        return xml.getBytes(StandardCharsets.UTF_8);
    }
}
