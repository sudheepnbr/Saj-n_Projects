# ADR 0004 — PostgreSQL as authoritative state

## Status

Accepted

## Context

Replay, lockout, SSO sessions, and SLO correlation must work across instances. H2 cannot host this faithfully.

## Decision

PostgreSQL is the only production and automated-test database for security state. Spring Session JDBC + dedicated protocol tables. Flyway is PostgreSQL-only.

H2 is a non-default local profile that **refuses** to enable SAML signing.

## Consequences

- Developers need Docker or a local Postgres.
- Tests use Testcontainers 2.0.5 (Boot 4.1.2 BOM).
