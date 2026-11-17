# Contributing

This project is developed in gated phases. Do not skip a phase.

| Phase | Status | Gate |
| ----- | ------ | ---- |
| 1 Architecture and threat model | Complete | Accepted |
| 2 Core identity platform | Complete | Phase 1 approved |
| 3 HSM integration | Complete | Phase 2 accepted |
| 4 MFA and sessions | Complete | Phase 3 accepted |
| 5 SAML IdP | Complete | Phase 4 accepted |
| 6 SLO and interoperability | Complete | Phase 5 accepted |
| 7 Production readiness | Complete | Phase 6 accepted |

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

Architecture documents and ADRs remain the source of truth. If implementation needs a different design, write an ADR first.
