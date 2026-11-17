# InfraMindTech IdP with FortiGate IPsec VPN

This IdP authenticates users with SAML 2.0. **IPsec is terminated on FortiGate**, not on the IdP. FortiClient builds the tunnel after SAML login.

Do **not** enable FortiGate SSL-VPN for this design.

Required: **FortiOS 7.2.4+** (7.4+ recommended) and **FortiClient 7.2+**. A browser alone cannot bring up IPsec.

Replace placeholders:

| Placeholder | Meaning | Lab example |
| ----------- | ------- | ----------- |
| `<IDP>` | Address users and FortiGate use to reach the IdP | `http://192.168.1.240:8080` |
| `<FGT>` | FortiGate FQDN or WAN IP (HTTPS) | `https://192.168.1.1` or `https://fgt.company.local` |

Production IdP URLs must be `https://`. Lab HTTP is only for a private LAN.

```
FortiClient IPsec  →  FortiGate IKE
                         │
                         │ SAML (browser)
                         ▼
                  InfraMindTech /login
                         │
                         │ assertion (attribute uid = username)
                         ▼
                  FortiGate  →  IPsec up
```

---

## 1. IdP (this software)

### 1.1 Start

```bash
git clone -b cursor/saml-mfa-idp-phases-3-7-04b9 https://github.com/sudheepnbr/Saj-n_Projects.git
cd Saj-n_Projects
./run.sh
```

If people open the IdP as `http://192.168.1.240:8080` (not localhost), set both of these in `docker-compose.yml` and recreate (`docker compose up --build -d`):

```yaml
OIH_ENTITY_ID: http://192.168.1.240:8080/saml2/idp/metadata
OIH_BASE_URL: http://192.168.1.240:8080
```

Wrong values fail SAML Destination checks.

### 1.2 First administrator

1. Open `<IDP>/setup`
2. Token: `oih-local-setup` (lab only)
3. Create an admin (password ≥ 12 characters, three character classes)
4. Sign in at `<IDP>/login`

### 1.3 VPN users

**Admin → Users → Create user**

- **Username** is what FortiGate must use as the VPN username (SAML attribute `uid`)
- Role **USER** for VPN staff; **ADMIN** only for IdP operators

Optional MFA: the user signs in, opens **Account → Enroll authenticator**, scans the QR with Microsoft or Google Authenticator, and stores the recovery codes.

### 1.4 Register FortiGate as a service provider

**Admin → Service providers**

Leave **Require signed AuthnRequest** and **Encrypt assertions** **off**. This IdP does not yet import a FortiGate signing certificate.

| Field | Value |
| ----- | ----- |
| Entity ID | `http://<FGT-host>/remote/saml/metadata` |
| Display name | `FortiGate IPsec` |
| ACS (HTTP-POST) | `https://<FGT-host>/remote/saml/login` |
| SLO (HTTP-POST) | `https://<FGT-host>/remote/saml/logout` |

ACS must match FortiGate **exactly** (scheme, host, port, path).

Then open **IdP metadata**: `<IDP>/saml2/idp/metadata`

Copy:

- Entity ID
- SSO URL: `<IDP>/saml2/idp/sso`
- SLO URL: `<IDP>/saml2/idp/slo`
- Signing certificate (`ds:X509Certificate`)

**Admin → Signing keys** shows the **ACTIVE** fingerprint if FortiGate asks for one.

### 1.5 What the IdP sends

| SAML field | Value | FortiGate |
| ---------- | ----- | --------- |
| NameID | Opaque UUID | Do **not** map as username |
| Attribute `uid` | Directory username | Map as **User Name** |
| Attribute `email` | Directory email | Optional |

---

## 2. FortiGate (IPsec)

FortiGate must reach `<IDP>`. Users’ PCs must reach **both** FortiGate and the IdP. Sync NTP (IdP allows ±2 minutes).

Do **not** configure SSL-VPN Settings or SSL-VPN portals for this design.

### 2.1 Import the IdP certificate

Paste the X.509 from IdP metadata as a remote certificate, for example `IMT-IDP`.

### 2.2 SAML single sign-on object

**User & Authentication → Single Sign-On** (or **User & Device → SAML**):

| FortiGate field | Value |
| --------------- | ----- |
| Name | `InfraMindTech` |
| SP entity ID | `http://<FGT-host>/remote/saml/metadata` |
| SP ACS URL | `https://<FGT-host>/remote/saml/login` |
| SP SLO URL | `https://<FGT-host>/remote/saml/logout` |
| IdP type | Custom |
| IdP entity ID | `<IDP>/saml2/idp/metadata` |
| IdP single sign-on URL | `<IDP>/saml2/idp/sso` |
| IdP single logout URL | `<IDP>/saml2/idp/slo` |
| IdP certificate | `IMT-IDP` |
| Username attribute | `uid` |
| Group attribute | empty unless you add groups later |

Bindings: HTTP-POST for ACS. This IdP accepts Redirect or POST for AuthnRequest.

```text
config user saml
    edit "InfraMindTech"
        set entity-id "http://FGT-HOST/remote/saml/metadata"
        set single-sign-on-url "https://FGT-HOST/remote/saml/login"
        set single-logout-url "https://FGT-HOST/remote/saml/logout"
        set idp-entity-id "http://192.168.1.240:8080/saml2/idp/metadata"
        set idp-single-sign-on-url "http://192.168.1.240:8080/saml2/idp/sso"
        set idp-single-logout-url "http://192.168.1.240:8080/saml2/idp/slo"
        set idp-cert "IMT-IDP"
        set user-name "uid"
        set digest-method sha256
    next
end

config user group
    edit "SAML-IPsec"
        set member "InfraMindTech"
    next
end
```

The SAML ACS path stays `/remote/saml/login` for IPsec. That is FortiOS user authentication, not SSL-VPN.

### 2.3 Dialup IPsec (IKEv2 + EAP, group = SAML)

Replace `wan1`, the PSK, and the client pool.

```text
config vpn ipsec phase1-interface
    edit "IMT-IPsec"
        set type dynamic
        set interface "wan1"
        set ike-version 2
        set peertype any
        set net-device disable
        set mode-cfg enable
        set proposal aes256-sha256
        set dhgrp 14
        set eap enable
        set eap-identity send-request
        set authusrgrp "SAML-IPsec"
        set ipv4-start-ip 10.212.134.10
        set ipv4-end-ip 10.212.134.200
        set dns-mode auto
        set psksecret ENC <strong-psk>
    next
end

config vpn ipsec phase2-interface
    edit "IMT-IPsec"
        set phase1name "IMT-IPsec"
        set proposal aes256-sha256
        set pfs enable
        set dhgrp 14
    next
end
```

Assign an address to the tunnel interface. Add a firewall policy: source `SAML-IPsec` / tunnel addresses → destination LAN.

---

## 3. Client (FortiClient)

Users need **FortiClient IPsec**, not the SSL-VPN web page.

1. An IdP account (username/password; optional TOTP).
2. FortiClient **Remote Access → add IPsec VPN** (not SSL-VPN).
3. Profile:
   - Remote gateway: FortiGate WAN IP or FQDN
   - Authentication: **SAML**
   - IKE version **2**
   - Pre-shared key: same as phase1 `psksecret`
   - Encryption / hash / DH: AES256 / SHA256 / DH14 (must match FortiGate)
4. **Connect** → FortiClient opens InfraMindTech `/login` → username and password (and TOTP if enrolled) → IPsec comes up.

Daily VPN is **FortiClient → Connect**. `/account` is only for profile and MFA enrollment.

---

## 4. Check order when it fails

1. From a PC, open `<IDP>/saml2/idp/metadata`.
2. From FortiGate, confirm routing to the IdP host.
3. ACS on the IdP equals FortiGate SSO URL, character for character.
4. FortiGate username attribute is `uid`.
5. `OIH_ENTITY_ID` and `OIH_BASE_URL` are the URL in the browser, not `localhost`, if users are on the LAN.
6. FortiClient is IPsec + IKEv2 + SAML, not SSL-VPN.
7. FortiOS is 7.2.4 or newer.

Lab Docker uses a **software signing token** and setup token `oih-local-setup`. Do not put that on the internet. Production: PKCS#11, unique secrets, HTTPS — see [`PRODUCTION.md`](PRODUCTION.md).
