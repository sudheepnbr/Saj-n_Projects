# Operations and high availability

## Deployment shape

```
                    ┌─────────────┐
  Users / SPs ─────►│ TLS proxy   │
                    │ (nginx)     │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ IdP node(s) │◄── PKCS#11 ── SoftHSM or net HSM
                    │ Spring Boot │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ PostgreSQL  │
                    │ primary     │
                    └─────────────┘
```

v1 HA: **stateless app nodes + shared PostgreSQL + shared network HSM**.  
v1 with SoftHSM: **single app node** (or active/passive with token dir on the active host only). Do not put SoftHSM token directories on NFS.

## Production invariants (fail startup)

- `SPRING_PROFILES_ACTIVE=prod`
- PostgreSQL reachable
- Flyway succeeded
- HSM PIN present
- Active signing key present and sign-self-test passed
- No default admin password
- `server.forward-headers-strategy` matches the proxy
- ACS URLs HTTPS
- `OIH_TOTP_WRAP_KEY` present (32-byte key, base64)
- JSON logging on

## Environment variables (names only)

| Name | Purpose |
| ---- | ------- |
| `OIH_HSM_PIN` | PKCS#11 user PIN |
| `OIH_HSM_LIBRARY` | Path to PKCS#11 .so |
| `OIH_HSM_SLOT` | Slot index or label |
| `OIH_TOTP_WRAP_KEY` | Envelope key |
| `OIH_RECOVERY_PEPPER` | Recovery-code HMAC pepper if used |
| `SPRING_DATASOURCE_URL` | JDBC |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `OIH_ENTITY_ID` | IdP entity ID |
| `OIH_BASE_URL` | Public base URL |

Values never appear in git. `.env.example` will list names in Phase 2.

## TLS

- Terminate TLS at the proxy. Minimum TLS 1.2, prefer 1.3.
- IdP may listen HTTP on loopback behind the proxy, or HTTPS itself.
- HSTS at the proxy.
- SAML endpoints must be the public HTTPS URLs in metadata (entityID, SSO, SLO). Wrong base URL = Destination failures.

## Health

| Endpoint | Checks |
| -------- | ------ |
| `/actuator/health/liveness` | Process up |
| `/actuator/health/readiness` | DB + HSM sign self-test |
| `/actuator/prometheus` | Authenticated or network-restricted |

Never expose Actuator to the internet without auth.

## Multi-instance implications

| Concern | Approach |
| ------- | -------- |
| HTTP sessions | Spring Session JDBC |
| Replay | `replay_cache` unique insert |
| Lockout | row updates on `users` / throttle table |
| Metadata | each node generates from DB key rows |
| Signing | each node uses PKCS#11 to same key (network HSM) or only one node with SoftHSM |
| Flyway | one winner at startup; others wait |
| Clocks | NTP on every node; skew > 2 min → readiness fail optional alert |

## Backup and recovery

**Recovery unit:** PostgreSQL dump + HSM token backup + `softhsm2.conf` + secret manager snapshot (PINs, wrap keys) + TLS material.

Restore procedure:

1. Restore secrets.
2. Restore HSM token (or reconnect network HSM — keys should still be there).
3. Restore PostgreSQL to the **same** backup generation.
4. Start one IdP node, verify readiness, compare metadata fingerprints to last known good.
5. If DB is newer than HSM (key row points at missing CKA_ID) → do not start; restore matching pair.

Point-in-time DB restore without HSM sync is a **signing outage**.

## Disaster: lost HSM

1. Generate new key in new token.
2. Publish as `NEXT` then immediately `ACTIVE` (emergency).
3. Distribute new metadata / cert to every SP **out of band**.
4. Old assertions still in flight will fail SP verification after TTL (minutes). SSO outage until SPs update IdP cert.

This is why overlapping rotation exists for planned events.

## JVM

- Java 25
- `-XX:+UseG1GC` or ZGC per sizing
- `spring.threads.virtual.enabled=true`
- PKCS#11 signer pool: platform threads, size 4–16
- Heap sized for XML unmarshalling spikes, not for keys
- Never `-Djavax.net.debug=ssl` in prod logs long-term

## Logging

JSON via Logback encoder:

- timestamp, level, logger, correlation_id, user_id (if any), sp_entity_id (if any), event

Deny-list filters for `password`, `pin`, `otp`, `secret`, `cookie`, `authorization`.

Audit events go to PostgreSQL **and** a log line with the event id (not the secrets).

## Upgrade / rollback

- Flyway versions are forward-only in prod. Rollback = restore recovery unit.
- Application rollback is safe if schema is compatible; do not expand-contract in a single unchecked step.
- Key rotation is independent of app version.

## Monitoring alerts

- Readiness down
- HSM sign failures
- Certificate `notAfter` < 30 days
- Lockout storms
- Replay reject spikes
- SLO partial rate
- Disk for SoftHSM token dir
- PostgreSQL connections
