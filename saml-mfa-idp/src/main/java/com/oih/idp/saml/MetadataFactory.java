package com.oih.idp.saml;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.crypto.SigningService;
import com.oih.idp.keys.SigningKey;
import com.oih.idp.sp.SpAcs;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class MetadataFactory {

    private final IdpProperties properties;
    private final SigningService signing;

    public MetadataFactory(IdpProperties properties, SigningService signing) {
        this.properties = properties;
        this.signing = signing;
    }

    public Document entity() {
        Document doc = SamlSupport.newDocument();
        Element entity = SamlSupport.element(doc, SamlSupport.MD, "EntityDescriptor");
        SamlSupport.attr(entity, "entityID", properties.getEntityId());
        SamlSupport.attr(entity, "ID", "_" + UUID.randomUUID().toString().replace("-", ""));
        SamlSupport.attr(entity, "validUntil", Instant.now().plusSeconds(86400 * 7).toString());
        doc.appendChild(entity);
        Element idp = SamlSupport.element(doc, SamlSupport.MD, "IDPSSODescriptor");
        SamlSupport.attr(idp, "protocolSupportEnumeration", "urn:oasis:names:tc:SAML:2.0:protocol");
        SamlSupport.attr(idp, "WantAuthnRequestsSigned", String.valueOf(properties.getSaml().isWantAuthnRequestsSigned()));
        for (SigningKey key : signing.metadataKeys()) {
            idp.appendChild(keyDescriptor(doc, signing.certificateOf(key)));
        }
        idp.appendChild(nameId(doc, "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"));
        idp.appendChild(nameId(doc, "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"));
        idp.appendChild(sso(doc, "SingleSignOnService", SpAcs.REDIRECT, "/saml2/idp/sso"));
        idp.appendChild(sso(doc, "SingleSignOnService", SpAcs.POST, "/saml2/idp/sso"));
        idp.appendChild(sso(doc, "SingleLogoutService", SpAcs.REDIRECT, "/saml2/idp/slo"));
        idp.appendChild(sso(doc, "SingleLogoutService", SpAcs.POST, "/saml2/idp/slo"));
        entity.appendChild(idp);
        SamlSupport.sign(entity, signing.activeCertificate(), signing.activePrivateKey());
        return doc;
    }

    private Element keyDescriptor(Document doc, X509Certificate certificate) {
        try {
            Element descriptor = SamlSupport.element(doc, SamlSupport.MD, "KeyDescriptor");
            SamlSupport.attr(descriptor, "use", "signing");
            Element keyInfo = doc.createElementNS("http://www.w3.org/2000/09/xmldsig#", "ds:KeyInfo");
            Element x509 = doc.createElementNS("http://www.w3.org/2000/09/xmldsig#", "ds:X509Data");
            Element cert = doc.createElementNS("http://www.w3.org/2000/09/xmldsig#", "ds:X509Certificate");
            cert.setTextContent(Base64.getEncoder().encodeToString(certificate.getEncoded()));
            x509.appendChild(cert);
            keyInfo.appendChild(x509);
            descriptor.appendChild(keyInfo);
            return descriptor;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Element nameId(Document doc, String value) {
        Element format = SamlSupport.element(doc, SamlSupport.MD, "NameIDFormat");
        format.setTextContent(value);
        return format;
    }

    private Element sso(Document doc, String local, String binding, String path) {
        Element service = SamlSupport.element(doc, SamlSupport.MD, local);
        SamlSupport.attr(service, "Binding", binding);
        SamlSupport.attr(service, "Location", properties.getBaseUrl() + path);
        return service;
    }
}
