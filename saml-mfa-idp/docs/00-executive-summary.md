# Executive summary

## Product

InfraMindTech Identity Provider is a **local-identity-only SAML 2.0 Identity Provider**. It is designed as an enterprise identity product, not a demonstration. The first deployment may be a single instance; the architecture assumes multiple instances from day one.

## Architectural recommendation

Build a **modular monolith**:

- one Spring Boot 4.1.2 process
- Java 25 virtual threads for HTTP handling, with PKCS#11 treated as a pinning-sensitive native boundary
- PostgreSQL as the single authoritative store for identities, sessions, replay, lockout, and audit
- SoftHSM2 behind a PKCS#11 `SigningService` so a physical HSM can replace it without redesigning protocol code
- OpenSAML 5.2.3 for all SAML object, signature, encryption, and metadata work
- Spring Security 7.1.2 for application authentication, sessions, CSRF, headers, and method security — **not** as a SAML IdP

Microservices are rejected for v1. SAML SSO, SLO, session revocation, and HSM signing share a transactional consistency model that is harder, not easier, when split across services.

## Why this is Phase 1 only

Custom IdP failures cluster in three places that are expensive to retrofit:

1. **SAML trust model** — ACS allow-lists, signed requests, audience, destination, replay, metadata.
2. **HSM abstraction** — key generation, non-extractability, certificate binding, rotation overlap.
3. **Session architecture** — MFA-pending vs fully authenticated, IdP SSO vs per-SP sessions, distributed replay.

Those three are specified here before any Java code is written.

## Scope for v1

In:

- local users, Argon2id passwords, TOTP MFA, recovery codes
- SP-initiated and IdP-initiated SSO (HTTP-Redirect and HTTP-POST for requests; HTTP-POST for responses)
- SP-initiated and IdP-initiated SLO (front-channel Redirect and POST)
- signed responses and assertions; optional assertion encryption
- persistent, email, and transient NameIDs
- admin console and user self-service
- signed IdP metadata with active + rollover certificates
- Flyway migrations, health checks, structured audit

Deferred from v1 (documented, not forgotten):

- SAML artifact binding and SOAP back-channel SLO
- Enhanced Client or Proxy (ECP)
- SAML attribute query / artifact resolution
- OIDC/OAuth, LDAP, identity brokering
- user self-registration
- default periodic password expiry
- GraalVM native image (PKCS#11 + OpenSAML are a poor native-image fit)

## Success criteria for this phase

A reviewer can answer:

- what is trusted and what is not
- how MFA cannot be bypassed on the SAML path
- how keys are born, published, activated, and retired
- how two IdP nodes cannot double-accept a replayed AuthnRequest
- which SAML features are mandatory vs per-SP
- which original requirements were corrected and why
