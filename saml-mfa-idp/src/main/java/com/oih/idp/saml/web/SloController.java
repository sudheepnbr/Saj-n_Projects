package com.oih.idp.saml.web;

import com.oih.idp.audit.AuditService;
import com.oih.idp.config.IdpProperties;
import com.oih.idp.crypto.SigningService;
import com.oih.idp.saml.ReplayGuard;
import com.oih.idp.saml.SamlProtocolException;
import com.oih.idp.saml.SamlSupport;
import com.oih.idp.session.SsoSessionService;
import com.oih.idp.sp.ServiceProvider;
import com.oih.idp.sp.ServiceProviderRegistry;
import com.oih.idp.sp.SpAcs;
import com.oih.idp.sp.SpSession;
import com.oih.idp.sp.SpSessionService;
import com.oih.idp.sp.SpSlo;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Controller
public class SloController {

    private static final String SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";
    private static final String PARTIAL = "urn:oasis:names:tc:SAML:2.0:status:PartialLogout";

    private final ServiceProviderRegistry registry;
    private final ReplayGuard replay;
    private final SsoSessionService sso;
    private final SpSessionService spSessions;
    private final SigningService signing;
    private final AuditService audit;
    private final IdpProperties properties;

    public SloController(
            ServiceProviderRegistry registry,
            ReplayGuard replay,
            SsoSessionService sso,
            SpSessionService spSessions,
            SigningService signing,
            AuditService audit,
            IdpProperties properties) {
        this.registry = registry;
        this.replay = replay;
        this.sso = sso;
        this.spSessions = spSessions;
        this.signing = signing;
        this.audit = audit;
        this.properties = properties;
    }

    @GetMapping("/saml2/idp/slo")
    public String redirect(
            @RequestParam("SAMLRequest") String samlRequest,
            @RequestParam(value = "RelayState", required = false) String relayState,
            HttpServletRequest request,
            Model model) {
        return handle(SamlSupport.decodeRedirect(samlRequest), relayState, request, model);
    }

    @PostMapping("/saml2/idp/slo")
    public String post(
            @RequestParam("SAMLRequest") String samlRequest,
            @RequestParam(value = "RelayState", required = false) String relayState,
            HttpServletRequest request,
            Model model) {
        return handle(SamlSupport.decodePost(samlRequest), relayState, request, model);
    }

    @PostMapping("/account/logout-global")
    public String idpInitiated(Authentication authentication, HttpServletRequest request) {
        var session = sso.current(request);
        if (session != null) {
            List<SpSession> bound = spSessions.forIdpSession(session.getId());
            boolean partial = bound.size() > 1;
            sso.revoke(session.getId());
            if (partial) {
                audit.record("SLO", "PARTIAL", session.getUserId(), request, "SLO_PARTIAL");
            } else {
                audit.record("SLO", "SUCCESS", session.getUserId(), request, null);
            }
        }
        return "redirect:/login?logout";
    }

    private String handle(byte[] xml, String relayState, HttpServletRequest request, Model model) {
        Document document = SamlSupport.parse(xml);
        Element logout = document.getDocumentElement();
        if (logout == null || !"LogoutRequest".equals(logout.getLocalName())) {
            throw new SamlProtocolException("SAML_REQUEST_INVALID", "Not a LogoutRequest");
        }
        String entityId = SamlSupport.text(logout, SamlSupport.SAML, "Issuer");
        if (entityId == null) {
            throw new SamlProtocolException("SAML_REQUEST_INVALID", "Missing issuer");
        }
        ServiceProvider sp = registry
                .findEnabled(entityId)
                .orElseThrow(() -> new SamlProtocolException("SAML_UNREGISTERED", "Unknown SP"));
        String destination = SamlSupport.attr(logout, "Destination");
        if (destination != null && !destination.equals(properties.getBaseUrl() + "/saml2/idp/slo")) {
            throw new SamlProtocolException("SAML_DESTINATION", "Destination mismatch");
        }
        String id = SamlSupport.attr(logout, "ID");
        if (id == null) {
            throw new SamlProtocolException("SAML_REQUEST_INVALID", "Missing ID");
        }
        replay.consume(id, "LogoutRequest", Instant.now().plus(properties.getSaml().getRequestMaxAge()));
        var current = sso.current(request);
        boolean partial = false;
        if (current != null) {
            List<SpSession> bound = spSessions.forIdpSession(current.getId());
            partial = bound.size() > 1;
            for (SpSession session : bound) {
                spSessions.markLogout(session, session.getSpId().equals(sp.getId()) ? "LOGGED_OUT" : "PARTIAL");
            }
            sso.revoke(current.getId());
        }
        if (partial) {
            audit.record("SLO", "PARTIAL", current == null ? null : current.getUserId(), request, "SLO_PARTIAL");
        } else {
            audit.record("SLO", "SUCCESS", current == null ? null : current.getUserId(), request, null);
        }
        String sloUrl = sp.getSlo().stream()
                .filter(entry -> SpAcs.POST.equals(entry.getBinding()))
                .map(SpSlo::getUrl)
                .findFirst()
                .orElse(sp.defaultAcs(SpAcs.POST));
        Document response = logoutResponse(id, sp, partial);
        model.addAttribute("acs", sloUrl);
        model.addAttribute("samlResponse", SamlSupport.encodePost(SamlSupport.serialize(response)));
        model.addAttribute("relayState", relayState);
        return "saml/post";
    }

    private Document logoutResponse(String inResponseTo, ServiceProvider sp, boolean partial) {
        Document doc = SamlSupport.newDocument();
        Element response = SamlSupport.element(doc, SamlSupport.SAMLP, "LogoutResponse");
        SamlSupport.attr(response, "ID", "_" + UUID.randomUUID().toString().replace("-", ""));
        SamlSupport.attr(response, "Version", "2.0");
        SamlSupport.attr(response, "IssueInstant", Instant.now().toString());
        SamlSupport.attr(response, "InResponseTo", inResponseTo);
        String dest = sp.getSlo().stream().map(SpSlo::getUrl).findFirst().orElse(null);
        SamlSupport.attr(response, "Destination", dest);
        doc.appendChild(response);
        Element issuer = SamlSupport.element(doc, SamlSupport.SAML, "Issuer");
        SamlSupport.attr(issuer, "Format", "urn:oasis:names:tc:SAML:2.0:nameid-format:entity");
        issuer.setTextContent(properties.getEntityId());
        response.appendChild(issuer);
        Element status = SamlSupport.element(doc, SamlSupport.SAMLP, "Status");
        Element code = SamlSupport.element(doc, SamlSupport.SAMLP, "StatusCode");
        String value = partial && "PARTIAL".equalsIgnoreCase(properties.getSaml().getPartialLogoutMode())
                ? PARTIAL
                : SUCCESS;
        SamlSupport.attr(code, "Value", value);
        status.appendChild(code);
        response.appendChild(status);
        SamlSupport.sign(response, signing.activeCertificate(), signing.activePrivateKey());
        return doc;
    }
}
