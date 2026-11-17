# Operations guide (Phase 7)

## Topology

TLS proxy → one or more IdP nodes → PostgreSQL. SoftHSM is **single-node**. A network HSM is required for active/active.

## Fail-fast production checks

`SPRING_PROFILES_ACTIVE=prod` refuses:

- H2 / in-memory JDBC
- default datasource passwords
- missing `OIH_SETUP_TOKEN`
- `idp.crypto.token` other than `pkcs11`
- missing `OIH_HSM_PIN`
- missing `OIH_TOTP_WRAP_KEY`

Readiness (`/actuator/health/readiness`) includes the HSM sign-self-test. An HSM outage marks the node not ready.

## Backup and restore

1. `pg_dump` of `oih_idp` (custom format).
2. Archive SoftHSM token directory **only** with the same node; never NFS.
3. Restore drill: `pg_restore` into a scratch database, start the app with `SPRING_DATASOURCE_URL` pointing at it, confirm `/login` and `/saml2/idp/metadata`.
4. Automated check: `BackupRestoreIT`.

## TLS

Terminate TLS 1.2+ at the proxy. Set `OIH_BASE_URL` and `OIH_ENTITY_ID` to the public HTTPS URLs. Wrong base URL breaks Destination checks.

## Secrets

| Name | Store |
| ---- | ----- |
| `OIH_HSM_PIN` | secret manager |
| `OIH_TOTP_WRAP_KEY` | 32-byte base64 envelope key |
| `SPRING_DATASOURCE_PASSWORD` | secret manager |
| `OIH_SETUP_TOKEN` | one-time bootstrap |

## Dependency scanning

Run `mvn -q org.owasp:dependency-check-maven:check` in CI (optional) and keep versions pinned to [`../reference/version-baseline.md`](../reference/version-baseline.md).

## Rate limits

Login IP throttle is in PostgreSQL (`auth_source_throttle`). SAML bodies larger than 256 KiB are rejected with HTTP 400.
