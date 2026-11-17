package com.oih.idp.saml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.Role;
import com.oih.idp.identity.domain.User;
import com.oih.idp.session.MfaPendingAuthenticationToken;
import com.oih.idp.sp.ServiceProviderRegistry;
import com.oih.idp.sp.SpAcs;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@AutoConfigureMockMvc
class SamlSsoIT extends PostgresIntegrationTest {

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
                "samluser", "saml@example.test", "Saml", "User", "CorrectHorseBattery1!", Set.of(Role.USER));
        principal = new IdpUserPrincipal(user);
    }

    @Test
    void metadataContainsActiveSigningKey() throws Exception {
        String xml = mvc.perform(get("/saml2/idp/metadata"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(xml).contains("IDPSSODescriptor");
        assertThat(xml).contains("X509Certificate");
        assertThat(xml).contains("Signature");
    }

    @Test
    void happyRedirectSsoIssuesSignedResponse() throws Exception {
        String encoded = SamlSupport.encodeRedirect(authn(SP, ACS, Instant.now()));
        MvcResult result = mvc.perform(get("/saml2/idp/sso")
                        .param("SAMLRequest", encoded)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("SAMLResponse");
        String b64 = html.replaceAll("(?s).*name=\"SAMLResponse\" value=\"([^\"]+)\".*", "$1");
        Document response = SamlSupport.parse(SamlSupport.decodePost(b64));
        Element root = response.getDocumentElement();
        assertThat(root.getLocalName()).isEqualTo("Response");
        assertThat(SamlSupport.child(root, SamlSupport.SAML, "Assertion")).isNotNull();
        assertThat(SamlSupport.text(root, SamlSupport.SAML, "Audience")).isEqualTo(SP);
        assertThat(SamlSupport.signatureAlgorithm(root)).isEqualTo(SamlSupport.RSA_SHA256);
        assertThat(SamlSupport.attr(root, "InResponseTo")).isNotBlank();
    }

    @Test
    void unregisteredIssuerIsRejected() throws Exception {
        String encoded = SamlSupport.encodeRedirect(authn("https://evil.example/metadata", ACS, Instant.now()));
        mvc.perform(get("/saml2/idp/sso").param("SAMLRequest", encoded).with(user(principal)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void badAcsIsRejected() throws Exception {
        String encoded = SamlSupport.encodeRedirect(authn(SP, "https://attacker.example/acs", Instant.now()));
        mvc.perform(get("/saml2/idp/sso").param("SAMLRequest", encoded).with(user(principal)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idpInitiatedHasNoInResponseTo() throws Exception {
        MvcResult result = mvc.perform(get("/saml2/idp/initiate").param("sp", SP).with(user(principal)))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();
        String b64 = html.replaceAll("(?s).*name=\"SAMLResponse\" value=\"([^\"]+)\".*", "$1");
        Document response = SamlSupport.parse(SamlSupport.decodePost(b64));
        assertThat(SamlSupport.attr(response.getDocumentElement(), "InResponseTo")).isNull();
    }

    @Test
    void csrfIsNotRequiredForSamlPost() throws Exception {
        String encoded = SamlSupport.encodePost(authn(SP, ACS, Instant.now()));
        mvc.perform(post("/saml2/idp/sso").param("SAMLRequest", encoded).with(user(principal)))
                .andExpect(status().isOk());
    }

    @Test
    void mfaPendingCannotIssueAssertion() throws Exception {
        String encoded = SamlSupport.encodeRedirect(authn(SP, ACS, Instant.now()));
        mvc.perform(get("/saml2/idp/sso")
                        .param("SAMLRequest", encoded)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                new MfaPendingAuthenticationToken(principal, false))))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/account")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                new MfaPendingAuthenticationToken(principal, false))))
                .andExpect(status().isForbidden());
    }

    @Test
    void replayedAuthnRequestIsRejected() throws Exception {
        byte[] xml = authn(SP, ACS, Instant.now());
        String encoded = SamlSupport.encodeRedirect(xml);
        mvc.perform(get("/saml2/idp/sso").param("SAMLRequest", encoded).with(user(principal)))
                .andExpect(status().isOk());
        mvc.perform(get("/saml2/idp/sso").param("SAMLRequest", encoded).with(user(principal)))
                .andExpect(status().isBadRequest());
    }

    static byte[] authn(String entityId, String acs, Instant issued) {
        String id = "_" + UUID.randomUUID().toString().replace("-", "");
        String xml = """
                <samlp:AuthnRequest xmlns:samlp="%s" xmlns:saml="%s" ID="%s" Version="2.0" IssueInstant="%s" Destination="http://localhost/saml2/idp/sso" AssertionConsumerServiceURL="%s" ProtocolBinding="%s"><saml:Issuer Format="urn:oasis:names:tc:SAML:2.0:nameid-format:entity">%s</saml:Issuer></samlp:AuthnRequest>
                """
                .formatted(SamlSupport.SAMLP, SamlSupport.SAML, id, issued, acs, SpAcs.POST, entityId);
        return xml.getBytes(StandardCharsets.UTF_8);
    }
}
