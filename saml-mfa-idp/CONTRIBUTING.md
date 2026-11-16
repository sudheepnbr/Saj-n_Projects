# Contributing

This project is developed in gated phases. Do not skip a phase.

| Phase | Status | Gate |
| ----- | ------ | ---- |
| 1 Architecture and threat model | Complete | Accepted on `main` |
| 2 Core identity platform | In review | This pull request |
| 3 HSM integration | Not started | Phase 2 accepted |
| 4 MFA and sessions | Not started | Phase 3 accepted |
| 5 SAML IdP | Not started | Phase 4 accepted |
| 6 SLO and interoperability | Not started | Phase 5 accepted |
| 7 Production readiness | Not started | Phase 6 accepted |

## Rules for later implementation phases

1. Use only the pinned versions in [`docs/reference/version-baseline.md`](docs/reference/version-baseline.md).
2. Do not invent Spring Security or OpenSAML APIs. Verify against current GA documentation.
3. Do not store SAML signing private keys outside the HSM.
4. Do not issue SAML assertions from an MFA-pending authentication state.
5. Do not accept an ACS URL that is not on the registered SP allow-list.
6. Do not send plaintext passwords to external breached-password services.
7. Keep secrets out of source control, logs, templates, and audit payloads.
8. Prefer PostgreSQL-backed state over in-memory caches for anything security-relevant.
9. Add or update the security test matrix when protocol behaviour changes.

## Documentation changes

Architecture documents are the source of truth until Phase 2 begins. If implementation needs a different design, write an ADR first.
