# InfraMindTech Identity Provider — documentation map

Read in this order.

1. [Executive summary](00-executive-summary.md) — product intent, scope, and the decision to stay a modular monolith.
2. [Specification review](01-specification-review.md) — what was wrong, missing, or too strong in the original requirements.
3. [Architecture blueprint](02-architecture-blueprint.md) — approved design.
4. [Module catalogue](03-module-responsibilities.md) — fourteen logical modules, dependencies, and controls.
5. [Trust boundaries and threat model](04-trust-boundaries-and-threat-model.md)
6. [Authentication and MFA](05-authentication-and-mfa.md)
7. [SAML 2.0 protocol](06-saml-protocol.md)
8. [Cryptography and HSM](07-cryptography-and-hsm.md)
9. [Sessions and Single Logout](08-sessions-and-slo.md)
10. [Data model](09-data-model.md)
11. [Operations and high availability](10-operations-and-ha.md)
12. [Testing strategy](11-testing-strategy.md)
13. [Implementation roadmap](12-implementation-roadmap.md)

Supporting material:

- [Approved requirements](spec/approved-requirements.md)
- [Version baseline](reference/version-baseline.md)
- [Interoperability matrix](reference/interoperability-matrix.md)
- [Security test matrix](reference/security-test-matrix.md)
- [Package structure](reference/package-structure.md)
- [Maven dependency design](reference/maven-dependency-design.md)
- [SoftHSM2 runbook](runbooks/softhsm2.md)
- [FortiGate IPsec SAML (IdP, FortiGate, FortiClient)](../../FORTIGATE.md)
- [ADRs](adr/README.md)

## Review gate

Phase 1 architecture is on `main`. Phases 2–7 are implemented on branch `cursor/saml-mfa-idp-phases-3-7-04b9`.

Phase 1 approval meant:

- the specification review findings are accepted or explicitly deferred
- the ADRs are accepted
- the interoperability matrix is accepted as the SAML contract
- residual risks in the threat model are accepted
