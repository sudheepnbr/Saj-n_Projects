# ADR 0011 — Apache Santuario XML security as the compile-time SAML stack

## Status

Accepted

## Context

ADR 0002 pins **OpenSAML 5.2.3** from the Shibboleth Maven repository as the IdP protocol library. That remains the architecture target.

This environment cannot resolve `https://build.shibboleth.net/maven/releases/` (HTTP 403). Blocking Phase 5–7 on a third-party Maven mirror would stall signing, MFA-gated assertions, SLO, and operations work that does not depend on OpenSAML class names.

OpenSAML itself uses Apache Santuario (`xmlsec`) and JDK XML Digital Signature for the actual XML Signature and Encryption work.

## Decision

- Keep OpenSAML 5.2.3 as the documented protocol pin and intended library when Shibboleth Nexus is reachable.
- Implement protocol objects with a hardened DOM parser (no DTD, no external entities, size cap) and sign/encrypt with Apache Santuario **4.0.4** plus JDK `XMLSignatureFactory`.
- Preserve SAML 2.0 behaviour: RSA-SHA256, Exclusive C14N, signed Response and Assertion, optional AES-256-GCM + RSA-OAEP encryption using the **registered SP** certificate, replay cache, ACS allow-list.
- Do not depend on `spring-security-saml2-service-provider` for IdP behaviour.

## Consequences

- Protocol tests assert XML structure and algorithms, not OpenSAML Java types.
- Switching the object layer back to OpenSAML 5.2.3 is an isolated change in `com.oih.idp.saml` once Nexus is available.
- Hand-built XML is constrained to `SamlSupport` / factories; parsers stay XXE-hardened.
