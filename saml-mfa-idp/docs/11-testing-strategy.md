# Testing strategy

No production code in Phase 1. This is the contract Phase 4–7 tests must satisfy. The explicit matrix is [`reference/security-test-matrix.md`](reference/security-test-matrix.md).

## Layers

| Layer | Tooling | Database / HSM |
| ----- | ------- | -------------- |
| Unit | JUnit 5 | None |
| Integration | Spring Boot Test, Testcontainers | PostgreSQL + SoftHSM2 |
| Protocol | OpenSAML client fixtures + independent SP | PostgreSQL + SoftHSM2 |
| Security | Dedicated tests from the matrix | PostgreSQL + SoftHSM2 |
| Interop | Keycloak-as-SP, Spring Security SP, optional Shibboleth / SimpleSAMLphp | Lab |

H2 is not an integration database for SAML, MFA, sessions, or lockout.

## Unit (examples)

- Password policy accept/reject
- Argon2id match / mismatch / rehash flag
- Lockout progression and cooldown
- TOTP window and timestep replay
- Recovery code consume-once
- Attribute mapper default-deny
- Session idle vs absolute expiry
- ReplayGuard unique conflict
- NameID persistent vs transient generation

## Integration

- Flyway migrates cleanly twice (idempotent)
- User uniqueness constraints
- PKCS#11 provider loads
- Sign and verify with token key
- `CKA_EXTRACTABLE` false
- Certificate public key matches token
- Encrypted assertion decryptable with SP test key
- Metadata contains active + next
- Spring Session persists across mock HTTP

## Protocol

- SP-initiated Redirect SSO
- SP-initiated POST SSO
- IdP-initiated POST SSO
- Signed AuthnRequest success
- Unsigned AuthnRequest rejected when required
- Wrong ACS rejected
- Wrong destination rejected
- Wrong audience would be an SP-side test; IdP still sets audience correctly
- Expired request rejected
- Replay rejected on two app instances sharing DB
- ForceAuthn forces password+MFA again
- IsPassive with no session returns NoPassive
- SP-initiated SLO
- IdP-initiated SLO partial timeout

## Independent SP lab (Phase 6)

Minimum:

1. A Spring Security 7 SAML **service provider** test app (this is what Spring Security actually is).
2. One of: Keycloak client/SP, SimpleSAMLphp, or Shibboleth SP.

FortiGate SSL-VPN SAML is an optional named interop target for OIH operations, not a v1 blocker.

## Security tests

See the matrix. Every row has an expected HTTP/SAML outcome and an audit event.

## What “green” means before production

- Unit + integration + protocol + security matrix pass in CI
- At least one independent SP SSO + SLO demonstrated, with screenshots or logs attached to the Phase 6 report
- OWASP dependency check / OSV scan on the pinned BOM with a documented exception list
- No secrets in git history
