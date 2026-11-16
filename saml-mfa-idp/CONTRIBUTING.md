# Contributing

This project is developed in gated phases. Do not skip a phase.

| Phase | Status | Gate |
| ----- | ------ | ---- |
| 1 Architecture and threat model | Complete | Accepted on `main` |
| 2 Core identity platform | Complete | Phase 2 PR |
| 3 HSM integration | In review | This pull request |
| 4 MFA and sessions | In review | This pull request |
| 5 SAML IdP | In review | This pull request |
| 6 SLO and interoperability | In review | This pull request |
| 7 Production readiness | In review | This pull request |

## Rules

1. Use only the pinned versions in [`docs/reference/version-baseline.md`](docs/reference/version-baseline.md).
2. Do not invent Spring Security APIs. Verify against current GA documentation.
3. Do not store SAML signing private keys outside the HSM / non-extractable token.
4. Do not issue SAML assertions from an MFA-pending authentication state.
5. Do not accept an ACS URL that is not on the registered SP allow-list.
6. Do not send plaintext passwords to external breached-password services.
7. Keep secrets out of source control, logs, templates, and audit payloads.
8. Prefer PostgreSQL-backed state over in-memory caches for anything security-relevant.
9. Add or update the security test matrix when protocol behaviour changes.
10. SAML XML must be parsed with the hardened factory in `SamlSupport` (no DTD, no external entities).

## Documentation changes

If implementation needs a different design, write an ADR first. ADR 0011 records the Santuario XML-security runtime used while OpenSAML 5.2.3 remains the architecture pin.
