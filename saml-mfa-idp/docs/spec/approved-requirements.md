# Approved requirements (v1)

Restatement of the master prompt after the specification review. If this file and the prompt disagree, **this file wins**.

## In scope

- Local-identity SAML 2.0 IdP
- Java 25, Spring Boot 4.1.2, Spring Security 7.1.2, OpenSAML 5.2.3
- PostgreSQL + Flyway; Testcontainers for tests
- SoftHSM2 via SunPKCS11, RSA-3072 SHA-256 signing
- Argon2id, TOTP, recovery codes
- SP- and IdP-initiated SSO; Redirect/POST requests; POST responses
- SP- and IdP-initiated front-channel SLO
- Admin console and `/account` portal
- Structured audit, key rotation with overlapping metadata
- Modular monolith

## Out of scope

- LDAP, AD, Entra ID, social login, OIDC provider, identity brokering
- Keycloak as a runtime dependency
- SAML artifact binding, ECP, attribute query, SOAP back-channel SLO
- User self-registration
- Default password expiry
- Remember-me
- GraalVM native image
- Claiming private keys never exist as JVM objects
- Spring Security SAML IdP APIs

## Security defaults

- MFA required before first assertion
- Signed responses and assertions
- ACS allow-list only
- Fail closed without HSM
- CSRF on except SAML POST receive endpoints
- SameSite=None; Secure when POST bindings on
- AES-256-GCM + RSA-OAEP-SHA256 for encryption, with per-SP CBC fallback
- Signing key `CKA_EXTRACTABLE=false`, `CKA_SENSITIVE=true`, `CKA_DECRYPT=false`

## Roles

| Role | Capability |
| ---- | ---------- |
| USER | Self-service, SSO |
| OPERATOR | View users/SPs, unlock, view sessions |
| ADMIN | Mutate users, SPs, keys, MFA reset |
| AUDITOR | Read audit and config, no secrets, no mutations |

Administrators must use MFA. Break-glass is a documented procedure, not an undocumented account.
