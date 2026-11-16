# Master engineering prompt (approved edition)

This is the development specification **after** the Principal Security Architect review. Use it for later coding agents. Do not silently revert to the uncorrected original.

The full architecture is in `docs/`. Non-negotiable deltas from the draft prompt:

1. Pin Spring Boot **4.1.2**, Spring Security **7.1.2**, OpenSAML **5.2.3**. Verify before bumping.
2. Implement the IdP with OpenSAML 5. Spring Security 7 is SP-only.
3. Add the Shibboleth Maven releases repository.
4. Sign with the IdP HSM key; encrypt with the **SP encryption certificate**.
5. Do not claim private keys never touch the JVM. Promise non-extractable PKCS#11 keys.
6. SAML Responses use HTTP-POST only.
7. CSRF remains on; SAML POST receive endpoints are explicit exceptions.
8. SameSite is `None; Secure` when POST bindings are enabled, never Strict as default.
9. PostgreSQL is the only security-state database. Testcontainers, not H2, for protocol tests.
10. MFA-pending tokens cannot issue assertions. Dual enforcement (filter + service).
11. AES-256-GCM default with per-SP CBC fallback. No RSA-1.5.
12. Virtual threads for HTTP; PKCS#11 on a platform-thread pool.
13. SoftHSM2 is the PKCS#11 backend, not a certified HSM. Token disk is secret.
14. No LDAP, Entra, OIDC provider, or Keycloak runtime.
15. Work in the gated roadmap. Phase 1 produced no Java. Phase 2 starts only after ADR acceptance.

If a later agent needs the narrative brief, read:

- [`../01-specification-review.md`](../01-specification-review.md)
- [`../02-architecture-blueprint.md`](../02-architecture-blueprint.md)
- [`approved-requirements.md`](approved-requirements.md)
- [`../12-implementation-roadmap.md`](../12-implementation-roadmap.md)
