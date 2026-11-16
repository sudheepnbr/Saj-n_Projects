package com.oih.idp.saml;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.crypto.SigningService;
import com.oih.idp.identity.domain.User;
import com.oih.idp.mapping.AttributeMapper;
import com.oih.idp.session.IdpSsoSession;
import com.oih.idp.sp.PersistentNameIdService;
import com.oih.idp.sp.ServiceProvider;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class AssertionFactory {

    private final IdpProperties properties;
    private final SigningService signing;
    private final AttributeMapper mapper;
    private final PersistentNameIdService nameIds;
    private final AssertionEncryptionService encryption;

    public AssertionFactory(
            IdpProperties properties,
            SigningService signing,
            AttributeMapper mapper,
            PersistentNameIdService nameIds,
            AssertionEncryptionService encryption) {
        this.properties = properties;
        this.signing = signing;
        this.mapper = mapper;
        this.nameIds = nameIds;
        this.encryption = encryption;
    }

    public Document build(
            User user,
            ServiceProvider sp,
            String acs,
            String inResponseTo,
            IdpSsoSession session,
            boolean mfaUsed) {
        Instant now = Instant.now();
        Instant notOnOrAfter = now.plus(properties.getSaml().getAssertionTtl()).plus(properties.getSaml().getClockSkew());
        Instant notBefore = now.minus(properties.getSaml().getClockSkew());
        Document doc = SamlSupport.newDocument();

        Element response = SamlSupport.element(doc, SamlSupport.SAMLP, "Response");
        String responseId = "_" + UUID.randomUUID().toString().replace("-", "");
        SamlSupport.attr(response, "ID", responseId);
        SamlSupport.attr(response, "Version", "2.0");
        SamlSupport.attr(response, "IssueInstant", now.toString());
        SamlSupport.attr(response, "Destination", acs);
        if (inResponseTo != null) {
            SamlSupport.attr(response, "InResponseTo", inResponseTo);
        }
        doc.appendChild(response);
        response.appendChild(issuer(doc));
        Element status = SamlSupport.element(doc, SamlSupport.SAMLP, "Status");
        Element code = SamlSupport.element(doc, SamlSupport.SAMLP, "StatusCode");
        SamlSupport.attr(code, "Value", "urn:oasis:names:tc:SAML:2.0:status:Success");
        status.appendChild(code);
        response.appendChild(status);

        Element assertion = SamlSupport.element(doc, SamlSupport.SAML, "Assertion");
        String assertionId = "_" + UUID.randomUUID().toString().replace("-", "");
        SamlSupport.attr(assertion, "ID", assertionId);
        SamlSupport.attr(assertion, "Version", "2.0");
        SamlSupport.attr(assertion, "IssueInstant", now.toString());
        assertion.appendChild(issuer(doc));
        assertion.appendChild(subject(doc, user, sp, acs, inResponseTo, notOnOrAfter));
        assertion.appendChild(conditions(doc, sp, notBefore, notOnOrAfter));
        assertion.appendChild(authn(doc, session, now, mfaUsed));
        assertion.appendChild(attributes(doc, user, sp));
        response.appendChild(assertion);
        SamlSupport.sign(assertion, signing.activeCertificate(), signing.activePrivateKey());
        if (sp.isEncryptAssertions()) {
            Element encrypted = encryption.encrypt(doc, assertion, sp);
            response.replaceChild(encrypted, assertion);
        }
        SamlSupport.sign(response, signing.activeCertificate(), signing.activePrivateKey());
        return doc;
    }

    private Element issuer(Document doc) {
        Element issuer = SamlSupport.element(doc, SamlSupport.SAML, "Issuer");
        SamlSupport.attr(issuer, "Format", "urn:oasis:names:tc:SAML:2.0:nameid-format:entity");
        issuer.setTextContent(properties.getEntityId());
        return issuer;
    }

    private Element subject(
            Document doc, User user, ServiceProvider sp, String acs, String inResponseTo, Instant notOnOrAfter) {
        Element subject = SamlSupport.element(doc, SamlSupport.SAML, "Subject");
        Element nameId = SamlSupport.element(doc, SamlSupport.SAML, "NameID");
        SamlSupport.attr(nameId, "Format", sp.getNameIdFormat());
        SamlSupport.attr(nameId, "SPNameQualifier", sp.getEntityId());
        nameId.setTextContent(nameIds.persistent(user, sp));
        subject.appendChild(nameId);
        Element confirmation = SamlSupport.element(doc, SamlSupport.SAML, "SubjectConfirmation");
        SamlSupport.attr(confirmation, "Method", "urn:oasis:names:tc:SAML:2.0:cm:bearer");
        Element data = SamlSupport.element(doc, SamlSupport.SAML, "SubjectConfirmationData");
        SamlSupport.attr(data, "Recipient", acs);
        SamlSupport.attr(data, "NotOnOrAfter", notOnOrAfter.toString());
        if (inResponseTo != null) {
            SamlSupport.attr(data, "InResponseTo", inResponseTo);
        }
        confirmation.appendChild(data);
        subject.appendChild(confirmation);
        return subject;
    }

    private Element conditions(Document doc, ServiceProvider sp, Instant notBefore, Instant notOnOrAfter) {
        Element conditions = SamlSupport.element(doc, SamlSupport.SAML, "Conditions");
        SamlSupport.attr(conditions, "NotBefore", notBefore.toString());
        SamlSupport.attr(conditions, "NotOnOrAfter", notOnOrAfter.toString());
        Element restriction = SamlSupport.element(doc, SamlSupport.SAML, "AudienceRestriction");
        Element audience = SamlSupport.element(doc, SamlSupport.SAML, "Audience");
        audience.setTextContent(sp.getEntityId());
        restriction.appendChild(audience);
        conditions.appendChild(restriction);
        return conditions;
    }

    private Element authn(Document doc, IdpSsoSession session, Instant now, boolean mfaUsed) {
        Element statement = SamlSupport.element(doc, SamlSupport.SAML, "AuthnStatement");
        SamlSupport.attr(statement, "AuthnInstant", now.toString());
        SamlSupport.attr(statement, "SessionIndex", session.getSessionIndex());
        Element context = SamlSupport.element(doc, SamlSupport.SAML, "AuthnContext");
        Element ref = SamlSupport.element(doc, SamlSupport.SAML, "AuthnContextClassRef");
        ref.setTextContent(
                mfaUsed
                        ? "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken"
                        : "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
        context.appendChild(ref);
        statement.appendChild(context);
        return statement;
    }

    private Element attributes(Document doc, User user, ServiceProvider sp) {
        Element statement = SamlSupport.element(doc, SamlSupport.SAML, "AttributeStatement");
        for (Map.Entry<String, String> entry : mapper.release(user, sp).entrySet()) {
            Element attribute = SamlSupport.element(doc, SamlSupport.SAML, "Attribute");
            SamlSupport.attr(attribute, "Name", entry.getKey());
            SamlSupport.attr(attribute, "NameFormat", "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
            Element value = SamlSupport.element(doc, SamlSupport.SAML, "AttributeValue");
            value.setTextContent(entry.getValue());
            attribute.appendChild(value);
            statement.appendChild(attribute);
        }
        return statement;
    }
}
