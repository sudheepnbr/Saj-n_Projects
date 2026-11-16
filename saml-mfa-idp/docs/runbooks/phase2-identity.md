# Phase 2 — run the identity platform

Phase 2 is the local-identity service: users, Argon2id passwords, persistent lockout, first-run `/setup`, `/login`, `/account`, and `/admin`. Later phases on the same binary add HSM signing, TOTP, SAML SSO/SLO, and production checks.

Production runtime target is **Java 25 LTS**. This codebase compiles to Java 21 bytecode so it also runs on Java 21–24.

## Prerequisites

- Java 21 or 25
- Maven 3.9+ (3.8.7 works)
- PostgreSQL 16+

Create the databases and role:

```sql
CREATE USER oih WITH PASSWORD 'oih_dev_only';
CREATE DATABASE oih_idp OWNER oih;
CREATE DATABASE oih_idp_test OWNER oih;
```

Copy [`.env.example`](../../.env.example) and export the variables, or rely on `application-dev.yml` defaults for local Postgres on `127.0.0.1:5432`.

## Run

```bash
cd saml-mfa-idp
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

On first boot the process logs a one-time setup token (development only). Open `/setup`, create the first administrator, then sign in at `/login`.

`prod` refuses H2, in-memory URLs, default datasource passwords, a missing `OIH_SETUP_TOKEN`, software token mode, a missing HSM PIN / TOTP wrap key, and non-HTTPS entity ID / base URL.

## Test

Default tests use PostgreSQL `oih_idp_test`. They do **not** use H2.

```bash
cd saml-mfa-idp
mvn test
```

| Test | Gate |
| ---- | ---- |
| `LoginEnumerationIT` | Unknown user and bad password share one public message |
| `LockoutIT` | Lockout is written to Postgres and later attempts see it |
| `AdminLifecycleIT` | Admin create / disable / unlock |
| `NoH2InClasspathTest` | `org.h2.Driver` is not on the test classpath |
