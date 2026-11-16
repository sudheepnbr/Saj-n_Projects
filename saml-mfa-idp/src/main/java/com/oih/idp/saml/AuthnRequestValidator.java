package com.oih.idp.saml;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.sp.ServiceProvider;
import com.oih.idp.sp.ServiceProviderRegistry;
import com.oih.idp.sp.SpAcs;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class AuthnRequestValidator {

    private final ServiceProviderRegistry registry;
    private final ReplayGuard replay;
    private final IdpProperties properties;

    public AuthnRequestValidator(
            ServiceProviderRegistry registry, ReplayGuard replay, IdpProperties properties) {
        this.registry = registry;
        this.replay = replay;
        this.properties = properties;
    }

    public ValidatedAuthn validate(Document document, String destination, String binding) {
        Element request = document.getDocumentElement();
        if (request == null || !"AuthnRequest".equals(request.getLocalName())) {
            throw new SamlProtocolException("SAML_REQUEST_INVALID", "Not an AuthnRequest");
        }
        String entityId = SamlSupport.text(request, SamlSupport.SAML, "Issuer");
        if (entityId == null) {
            throw new SamlProtocolException("SAML_REQUEST_INVALID", "Missing issuer");
        }
        ServiceProvider sp = registry
                .findEnabled(entityId)
                .orElseThrow(() -> new SamlProtocolException("SAML_UNREGISTERED", "Unknown SP"));
        String dest = SamlSupport.attr(request, "Destination");
        if (dest != null && !dest.equals(destination)) {
            throw new SamlProtocolException("SAML_DESTINATION", "Destination mismatch");
        }
        String acs = SamlSupport.attr(request, "AssertionConsumerServiceURL");
        if (acs == null) {
            acs = sp.defaultAcs(SpAcs.POST);
        }
        if (acs == null || (!sp.allowsAcs(acs, SpAcs.POST) && !sp.allowsAcs(acs, binding))) {
            throw new SamlProtocolException("SAML_ACS", "ACS is not allow-listed");
        }
        if (sp.isWantAuthnRequestsSigned()) {
            String alg = SamlSupport.signatureAlgorithm(request);
            if (alg == null) {
                throw new SamlProtocolException("SAML_SIG_FAIL", "Signed AuthnRequest required");
            }
            if (alg.contains("sha1") || SamlSupport.RSA_SHA1.equals(alg)) {
                throw new SamlProtocolException("SAML_ALG", "SHA-1 signatures are forbidden");
            }
            if (registry.signingCert(sp).isEmpty()) {
                throw new SamlProtocolException("SAML_SIG_FAIL", "No SP signing cert");
            }
        }
        String issuedRaw = SamlSupport.attr(request, "IssueInstant");
        if (issuedRaw == null) {
            throw new SamlProtocolException("SAML_REQUEST_INVALID", "Missing IssueInstant");
        }
        Instant issued = Instant.parse(issuedRaw);
        Instant now = Instant.now();
        if (issued.isBefore(now.minus(properties.getSaml().getRequestMaxAge().plus(properties.getSaml().getClockSkew())))
                || issued.isAfter(now.plus(properties.getSaml().getClockSkew()))) {
            throw new SamlProtocolException("SAML_STALE", "Stale AuthnRequest");
        }
        String id = SamlSupport.attr(request, "ID");
        if (id == null) {
            throw new SamlProtocolException("SAML_REQUEST_INVALID", "Missing ID");
        }
        replay.consume(
                id,
                "AuthnRequest",
                now.plus(properties.getSaml().getRequestMaxAge()).plus(properties.getSaml().getClockSkew()));
        boolean force = Boolean.parseBoolean(SamlSupport.attr(request, "ForceAuthn"));
        boolean passive = Boolean.parseBoolean(SamlSupport.attr(request, "IsPassive"));
        return new ValidatedAuthn(sp, id, acs, binding, force, passive);
    }

    public record ValidatedAuthn(
            ServiceProvider sp, String requestId, String acs, String binding, boolean forceAuthn, boolean isPassive) {
    }
}
