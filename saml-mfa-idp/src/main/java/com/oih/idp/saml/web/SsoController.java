package com.oih.idp.saml.web;

import com.oih.idp.audit.AuditService;
import com.oih.idp.config.IdpProperties;
import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.User;
import com.oih.idp.saml.AssertionFactory;
import com.oih.idp.saml.AuthnRequestValidator;
import com.oih.idp.saml.AuthnRequestValidator.ValidatedAuthn;
import com.oih.idp.saml.SamlProtocolException;
import com.oih.idp.saml.SamlSupport;
import com.oih.idp.session.AuthenticationStates;
import com.oih.idp.session.IdpSsoSession;
import com.oih.idp.session.SsoSessionService;
import com.oih.idp.sp.ServiceProvider;
import com.oih.idp.sp.ServiceProviderRegistry;
import com.oih.idp.sp.SpAcs;
import com.oih.idp.sp.SpSessionService;
import com.oih.idp.web.security.IdpUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Document;

@Controller
public class SsoController {

    public static final String CONTINUE = "SAML_CONTINUE";

    private final AuthnRequestValidator validator;
    private final AssertionFactory assertions;
    private final SsoSessionService sso;
    private final IdentityService identity;
    private final SpSessionService spSessions;
    private final ServiceProviderRegistry registry;
    private final AuditService audit;
    private final IdpProperties properties;

    public SsoController(
            AuthnRequestValidator validator,
            AssertionFactory assertions,
            SsoSessionService sso,
            IdentityService identity,
            SpSessionService spSessions,
            ServiceProviderRegistry registry,
            AuditService audit,
            IdpProperties properties) {
        this.validator = validator;
        this.assertions = assertions;
        this.sso = sso;
        this.identity = identity;
        this.spSessions = spSessions;
        this.registry = registry;
        this.audit = audit;
        this.properties = properties;
    }

    @GetMapping("/saml2/idp/sso")
    public String redirectSso(
            @RequestParam("SAMLRequest") String samlRequest,
            @RequestParam(value = "RelayState", required = false) String relayState,
            HttpServletRequest request,
            Authentication authentication,
            Model model) {
        return handle(SamlSupport.decodeRedirect(samlRequest), SpAcs.REDIRECT, relayState, request, authentication, model);
    }

    @PostMapping("/saml2/idp/sso")
    public String postSso(
            @RequestParam("SAMLRequest") String samlRequest,
            @RequestParam(value = "RelayState", required = false) String relayState,
            HttpServletRequest request,
            Authentication authentication,
            Model model) {
        if (samlRequest.length() > properties.getSaml().getMaxBodyBytes()) {
            throw new SamlProtocolException("SAML_OVERSIZE", "SAML message too large");
        }
        return handle(SamlSupport.decodePost(samlRequest), SpAcs.POST, relayState, request, authentication, model);
    }

    @GetMapping("/saml2/idp/initiate")
    public String initiate(
            @RequestParam("sp") String entityId,
            Authentication authentication,
            HttpServletRequest request,
            Model model) {
        if (!AuthenticationStates.isFullyAuthenticated(authentication)) {
            return "redirect:/login";
        }
        ServiceProvider sp = registry
                .findEnabled(entityId)
                .orElseThrow(() -> new SamlProtocolException("SAML_UNREGISTERED", "Unknown SP"));
        String acs = sp.defaultAcs(SpAcs.POST);
        if (acs == null) {
            throw new SamlProtocolException("SAML_ACS", "No POST ACS");
        }
        return issue(sp, acs, null, null, authentication, request, model);
    }

    private String handle(
            byte[] xml,
            String binding,
            String relayState,
            HttpServletRequest request,
            Authentication authentication,
            Model model) {
        if (xml.length > properties.getSaml().getMaxBodyBytes()) {
            throw new SamlProtocolException("SAML_OVERSIZE", "SAML message too large");
        }
        Document document = SamlSupport.parse(xml);
        String destination = properties.getBaseUrl() + "/saml2/idp/sso";
        ValidatedAuthn validated = validator.validate(document, destination, binding);
        request.getSession(true)
                .setAttribute(
                        "SAML_AUTHN",
                        new StoredAuthn(
                                validated.sp().getEntityId(),
                                validated.acs(),
                                validated.requestId(),
                                validated.forceAuthn()));
        request.getSession().setAttribute("SAML_RELAY", relayState);
        if (validated.isPassive() && !AuthenticationStates.isFullyAuthenticated(authentication)) {
            audit.record("SSO", "FAILURE", null, request, "SAML_NOPASSIVE");
            throw new SamlProtocolException("SAML_NOPASSIVE", "NoPassive");
        }
        if (!AuthenticationStates.isFullyAuthenticated(authentication) || validated.forceAuthn()) {
            request.getSession().setAttribute(CONTINUE, "/saml2/idp/continue");
            return "redirect:/login";
        }
        return issue(validated.sp(), validated.acs(), validated.requestId(), relayState, authentication, request, model);
    }

    @GetMapping("/saml2/idp/continue")
    public String resume(HttpServletRequest request, Authentication authentication, Model model) {
        if (!AuthenticationStates.isFullyAuthenticated(authentication)) {
            return "redirect:/login";
        }
        Object stored = request.getSession().getAttribute("SAML_AUTHN");
        if (!(stored instanceof StoredAuthn pending)) {
            return "redirect:/account";
        }
        ServiceProvider sp = registry
                .findEnabled(pending.spEntityId())
                .orElseThrow(() -> new SamlProtocolException("SAML_UNREGISTERED", "Unknown SP"));
        String relay = (String) request.getSession().getAttribute("SAML_RELAY");
        return issue(sp, pending.acs(), pending.requestId(), relay, authentication, request, model);
    }

    public record StoredAuthn(String spEntityId, String acs, String requestId, boolean forceAuthn)
            implements java.io.Serializable {}

    private String issue(
            ServiceProvider sp,
            String acs,
            String inResponseTo,
            String relayState,
            Authentication authentication,
            HttpServletRequest request,
            Model model) {
        IdpUserPrincipal principal = (IdpUserPrincipal) authentication.getPrincipal();
        User user = identity.requireUser(principal.getUserId());
        if (properties.getMfa().isEnabled() && sp.isMfaRequired() && !user.isMfaEnabled()) {
            throw new SamlProtocolException("SAML_NOPASSIVE", "MFA required");
        }
        IdpSsoSession session = sso.current(request);
        if (session == null) {
            session = sso.establish(principal, request);
        }
        boolean mfa = user.isMfaEnabled();
        Document response = assertions.build(user, sp, acs, inResponseTo, session, mfa);
        spSessions.bind(session, sp, user);
        audit.record("SSO", "SUCCESS", user.getId(), request, null);
        model.addAttribute("acs", acs);
        model.addAttribute("samlResponse", SamlSupport.encodePost(SamlSupport.serialize(response)));
        model.addAttribute("relayState", relayState);
        return "saml/post";
    }
}
