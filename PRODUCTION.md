# InfraMindTech Identity Provider — setup and production

Use this document to run the full stack in a **lab** and to cut over to **production**.
The downloadable ZIP contains the same files as branch `cursor/saml-mfa-idp-phases-3-7-04b9`.

Product: **InfraMindTech Identity Provider** — SAML 2.0 IdP for locally managed users, TOTP MFA, PKCS#11 signing, PostgreSQL. Vendor: **InfraMindTech**.

This product does **not** broker LDAP, Active Directory, Entra ID, social login, or OIDC.

---

## 1. What is in the package

| Path | Purpose |
| ---- | ------- |
| `START.md` | Fast local start |
| `PRODUCTION.md` | This guide |
| `docs/IMT-IdP-Setup-Instructions.pdf` | Printable copy of this guide |
| `run.sh` / `docker-compose.yml` | Local Docker (software signing token) |
| `docker-compose.prod.yml` | Production Docker (PKCS#11 + TLS proxy) |
| `deploy/env.prod.example` | Production environment template |
| `deploy/generate-secrets.sh` | Generate unique secrets |
| `deploy/Caddyfile` | TLS reverse proxy |
| `saml-mfa-idp/` | Application source, Dockerfile, Maven project |
| `saml-mfa-idp/scripts/run-dev.sh` | Local Maven + PostgreSQL |
| `saml-mfa-idp/scripts/reset-dev-db.sh` | Wipe local data so `/setup` returns |

---

## 2. Lab / local (not production)

**Need:** Docker Desktop, **or** Java 21+, Maven 3.9+, PostgreSQL 16+.

### Docker

```bash
chmod +x run.sh
./run.sh
```

Open http://localhost:8080/setup  
First-run token: **`oih-local-setup`**

Create the first administrator (password ≥ 12 characters, at least 3 of: upper, lower, digit, symbol).

| URL | Use |
| --- | --- |
| http://localhost:8080/login | Sign in |
| http://localhost:8080/account | Profile, optional MFA, IdP-initiated SSO |
| http://localhost:8080/admin | Users, service providers, signing keys |
| http://localhost:8080/saml2/idp/metadata | Public SAML metadata |

Local Docker uses the **software** signing token and well-known secrets (`oih-local-setup`, `oih_dev_only`, all-zero TOTP wrap key). Do not put this profile on the internet.

### Without Docker

```bash
# as a Postgres superuser:
CREATE USER oih WITH PASSWORD 'oih_dev_only';
CREATE DATABASE oih_idp OWNER oih;

chmod +x saml-mfa-idp/scripts/run-dev.sh
./saml-mfa-idp/scripts/run-dev.sh
```

Reset local data:

```bash
./saml-mfa-idp/scripts/reset-dev-db.sh
```

Then restart the IdP so it mints a fresh setup token.

---

## 3. Daily use after first-run

1. Sign in at `/login`.
2. **Admin → Users** to create people (role USER, OPERATOR, ADMIN, or AUDITOR).
3. **Admin → Service providers** to register each application:
   - Entity ID (exact, e.g. `https://app.example.com/saml/metadata`)
   - Display name
   - ACS URL (HTTP-POST), exact allow-list match
   - Optional SLO URL
4. The application appears on `/account` with **Sign in** (IdP-initiated SSO).
5. Give each SP the IdP metadata URL: `{base-url}/saml2/idp/metadata`.
6. Optional: **Enroll authenticator** on the account page (RFC 6238 TOTP). After enrollment, every sign-in asks for a code.

SAML HTTP-POST from an SP does not use CSRF. ACS URLs that are not registered never receive an assertion. MFA-pending sessions cannot issue assertions.

---

## 4. Production architecture

```
Users / SPs  →  Caddy (TLS 1.2+)  →  IdP (Spring Boot)  →  PostgreSQL
                                      └── PKCS#11 (SoftHSM2 or network HSM)
```

- Terminate HTTPS at the proxy. The app listens on HTTP inside the private network.
- `OIH_ENTITY_ID` and `OIH_BASE_URL` **must** be the public `https://` URLs. Wrong values fail SAML Destination checks.
- SoftHSM is **single-node**. Active/active needs a network HSM (Thales, nShield, CloudHSM) with the same PKCS#11 library path.
- Do not share a SoftHSM token directory over NFS.

`SPRING_PROFILES_ACTIVE=prod` refuses to start if any of these are true:

- H2 / in-memory JDBC
- default DB password (`oih_dev_only`, `password`, blank)
- missing or `change-me` setup token
- signing token is not `pkcs11`
- missing `OIH_HSM_PIN`
- missing `OIH_TOTP_WRAP_KEY`
- entity ID or base URL is not HTTPS

---

## 5. Production secrets (generate, never commit)

```bash
chmod +x deploy/generate-secrets.sh
./deploy/generate-secrets.sh > .env.prod
```

Edit `.env.prod`:

- Set `OIH_PUBLIC_HOST` to the DNS name (no scheme), e.g. `idp.example.com`
- Set `OIH_ENTITY_ID` to `https://idp.example.com/saml2/idp/metadata`
- Set `OIH_BASE_URL` to `https://idp.example.com`

Keep `.env.prod` in a secret manager. It is gitignored conceptually — **do not add it to git**.

| Variable | How to make it |
| -------- | -------------- |
| `OIH_SETUP_TOKEN` | `openssl rand -base64 24` — used once at `/setup` |
| `OIH_TOTP_WRAP_KEY` | `openssl rand -base64 32` — wraps TOTP secrets |
| `OIH_HSM_PIN` / `OIH_HSM_SO_PIN` | Unique PINs for the PKCS#11 token |
| `POSTGRES_PASSWORD` | Strong unique password (same as `SPRING_DATASOURCE_PASSWORD`) |

---

## 6. Production with Docker (recommended)

**Need:** a Linux host, Docker Compose, DNS A/AAAA record for `OIH_PUBLIC_HOST` pointing at the host, ports 80 and 443 open for Let’s Encrypt.

```bash
cp deploy/env.prod.example .env.prod
# or: ./deploy/generate-secrets.sh > .env.prod
# then edit hostnames

docker compose --env-file .env.prod -f docker-compose.prod.yml up --build -d
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f idp
```

Wait until the IdP logs show it is listening. Then:

1. Open `https://idp.example.com/setup`
2. Enter **your** `OIH_SETUP_TOKEN` (not `oih-local-setup`)
3. Create the first administrator
4. Sign in, register service providers, download metadata

Health (from the host or load balancer):

- `https://idp.example.com/actuator/health`
- Readiness includes a PKCS#11 sign self-test

Prometheus is **not** published by the proxy.

Stop:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down
```

Volumes `oih-idp-pg` and `oih-idp-hsm` hold the database and the signing token. Back them up together.

---

## 7. Production without Docker (host install)

1. Install **Java 21+** (Java 25 LTS is the architecture target), **PostgreSQL 16+**, **SoftHSM2**, **Caddy or nginx**.
2. Create database and role with a unique password.
3. Initialize SoftHSM (see `saml-mfa-idp/docs/runbooks/softhsm2.md`):

```bash
sudo apt-get install -y softhsm2
sudo mkdir -p /var/lib/oih-idp/tokens
sudo chown oih-idp:oih-idp /var/lib/oih-idp/tokens
softhsm2-util --init-token --free --label oih-idp \
  --so-pin "$OIH_HSM_SO_PIN" --pin "$OIH_HSM_PIN"
```

4. Build:

```bash
cd saml-mfa-idp
mvn -DskipTests package
```

5. Run with production environment (systemd unit recommended):

```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/oih_idp
export SPRING_DATASOURCE_USERNAME=oih
export SPRING_DATASOURCE_PASSWORD='...'
export OIH_SETUP_TOKEN='...'
export OIH_TOTP_WRAP_KEY='...'
export OIH_HSM_PIN='...'
export OIH_ENTITY_ID=https://idp.example.com/saml2/idp/metadata
export OIH_BASE_URL=https://idp.example.com
export OIH_HSM_LIBRARY=/usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so
java -jar target/oih-idp-0.3.0-SNAPSHOT.jar
```

6. Put Caddy/nginx in front. Forward `X-Forwarded-Proto` and `X-Forwarded-Host`. The `prod` profile already sets `server.forward-headers-strategy=framework` and `SameSite=None; Secure` cookies.

On first start the application generates a non-extractable RSA-3072 signing key inside the token and publishes it in metadata.

---

## 8. Connect a service provider

On the IdP:

1. Admin → Service providers → register entity ID, ACS (POST), optional SLO.
2. Copy IdP metadata from `/saml2/idp/metadata`.

On the SP (Shibboleth, SimpleSAMLphp, FortiGate **IPsec** SAML, custom app). For FortiGate IPsec (not SSL-VPN) see [`FORTIGATE.md`](FORTIGATE.md):

1. Import IdP metadata or set:
   - SSO URL: `https://idp.example.com/saml2/idp/sso`
   - SLO URL: `https://idp.example.com/saml2/idp/slo`
   - Entity ID: `https://idp.example.com/saml2/idp/metadata`
   - Signing certificate from metadata
2. ACS on the SP must **exactly** match the URL registered on the IdP.
3. Test SP-initiated SSO, then IdP-initiated **Sign in** from `/account`, then logout.

Unsigned AuthnRequests are accepted only when the SP is registered with “Require signed AuthnRequest” unchecked. Production SPs should send signed AuthnRequests.

---

## 9. Backup and restore

Back up **together**, same generation:

1. `pg_dump -Fc oih_idp`
2. SoftHSM token directory (`oih-idp-hsm` volume, or `/var/lib/softhsm/tokens`)
3. Secret values (`OIH_HSM_PIN`, `OIH_HSM_SO_PIN`, `OIH_TOTP_WRAP_KEY`) from the secret manager — not from git

Restore into a scratch database, start the app against it, confirm `/login` and `/saml2/idp/metadata` before declaring success.

---

## 10. Go-live checklist

- [ ] Unique secrets; no `oih-local-setup`, `oih_dev_only`, or all-zero wrap key
- [ ] DNS and HTTPS working; metadata URLs are `https://`
- [ ] `/setup` completed; setup token stored then treated as used
- [ ] First admin enrolled in TOTP
- [ ] At least one real SP SSO and SLO tested
- [ ] Backup/restore drill completed
- [ ] Actuator (except health) not on the public internet
- [ ] PostgreSQL not published on 0.0.0.0
- [ ] SoftHSM: single IdP node, or replaced with a network HSM for HA
- [ ] PRs merged only after review: https://github.com/sudheepnbr/Saj-n_Projects/pull/2

---

## 11. Support references

- Architecture: `saml-mfa-idp/docs/README.md`
- Operations: `saml-mfa-idp/docs/runbooks/operations.md`
- SoftHSM: `saml-mfa-idp/docs/runbooks/softhsm2.md`
- Source: https://github.com/sudheepnbr/Saj-n_Projects/tree/cursor/saml-mfa-idp-phases-3-7-04b9
