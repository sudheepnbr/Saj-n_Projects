# Run the IMT InfraMindTech Identity Provider

This is the **ready-to-run** entry point. Use the branch
`cursor/saml-mfa-idp-phases-3-7-04b9` (or [PR #2](https://github.com/sudheepnbr/Saj-n_Projects/pull/2)).
`main` is architecture only.

**Production cutover** (HTTPS, PKCS#11, unique secrets): [`PRODUCTION.md`](PRODUCTION.md) and the printable PDF [`docs/IMT-IdP-Setup-Instructions.pdf`](docs/IMT-IdP-Setup-Instructions.pdf).

## 1. Get the files

**ZIP**

https://github.com/sudheepnbr/Saj-n_Projects/archive/refs/heads/cursor/saml-mfa-idp-phases-3-7-04b9.zip

Unzip, then:

```bash
cd Saj-n_Projects-cursor-saml-mfa-idp-phases-3-7-04b9
```

**Git**

```bash
git clone -b cursor/saml-mfa-idp-phases-3-7-04b9 https://github.com/sudheepnbr/Saj-n_Projects.git
cd Saj-n_Projects
```

## 2. Start (Docker — recommended)

Install [Docker Desktop](https://www.docker.com/products/docker-desktop/). Then:

```bash
chmod +x run.sh
./run.sh
```

Or:

```bash
docker compose up --build
```

Wait until the IdP container is up (first build downloads Maven dependencies).

## 3. Open the app

| Step | URL | What to enter |
| ---- | --- | ------------- |
| First-run setup | http://localhost:8080/setup | Token **`oih-local-setup`**, then create the first admin (password ≥ 12 characters, 3 character classes) |
| Sign in | http://localhost:8080/login | The admin you just created |
| Account | http://localhost:8080/account | Profile, optional MFA, **Sign in** links for registered apps |
| Admin | http://localhost:8080/admin | Users, **Service providers**, **Signing keys** |
| SAML metadata | http://localhost:8080/saml2/idp/metadata | Public XML |

Register a service provider under Admin → Service providers (entity ID + ACS URL). It then appears on the account page with an IdP-initiated **Sign in** button.

This local stack uses the **software signing token** (not SoftHSM). That is for development only.

## 4. Without Docker

You need **Java 21+**, **Maven 3.9+**, and **PostgreSQL 16+**.

```bash
chmod +x saml-mfa-idp/scripts/run-dev.sh
./saml-mfa-idp/scripts/run-dev.sh
```

If the script cannot create the database, run this as a Postgres superuser first:

```sql
CREATE USER oih WITH PASSWORD 'oih_dev_only';
CREATE DATABASE oih_idp OWNER oih;
```

## Stop / reset

```bash
docker compose down
# wipe the database volume:
docker compose down -v
```

Without Docker, reset the local database (then restart the IdP) so `/setup` is offered again:

```bash
chmod +x saml-mfa-idp/scripts/reset-dev-db.sh
./saml-mfa-idp/scripts/reset-dev-db.sh
```

## Not for production

`oih-local-setup`, `oih_dev_only`, and the all-zero TOTP wrap key are **development secrets**. Production requires PKCS#11, `OIH_HSM_PIN`, `OIH_TOTP_WRAP_KEY`, HTTPS entity ID / base URL, and a unique setup token. See `saml-mfa-idp/docs/runbooks/operations.md`.
