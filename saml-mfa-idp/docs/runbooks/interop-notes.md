# Interoperability notes (Phase 6)

Captured against the IdP’s DOM + Apache Santuario 4.0.4 stack (OpenSAML 5.2.3 remains the architecture pin; see ADR 0011) and protocol integration tests. Live Spring Security / Shibboleth / SimpleSAMLphp labs should add rows here.

| Topic | IdP behaviour | SP implication |
| ----- | ------------- | -------------- |
| NameID | Default `persistent`, pairwise opaque UUID stored in `persistent_nameids` | Do not expect email NameID unless the SP registry row is changed |
| Signing | Response **and** Assertion, RSA-SHA256, Exclusive C14N | SHA-1 signatures are rejected |
| Encryption | Optional per SP; AES-256-GCM + RSA-OAEP using the **registry** encryption cert | Certificates in the AuthnRequest KeyInfo are ignored |
| Clock skew | ±2 minutes | SP should allow at least 2 minutes |
| ACS | Allow-list only | Wrong ACS never yields an assertion |
| SLO | Front-channel POST/Redirect; default status `Success` even on partial | Partial logout is audited as `SLO_PARTIAL` |
| POST binding | CSRF disabled only on `/saml2/idp/sso` and `/saml2/idp/slo` | Compensating SAML checks still apply |
