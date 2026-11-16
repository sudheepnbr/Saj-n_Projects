# Specification review

Role of this document: Principal Security Architect review of the master engineering prompt, before any implementation.

Verdict: the original specification is a strong IdP brief. It is **not** implementation-ready until the corrections below are adopted. Several items are technically incorrect, several are over-promised, and several enterprise IdP requirements are missing.

## 1. Incorrect or incompatible technical assumptions

### 1.1 Java 25 + Spring Boot 4.1 + Spring Security 7 is compatible — if pinned to GA

**Original:** assume Java 25, Spring Boot 4.1.x, Spring Security 7.x.

**Finding:** Compatible, but only with released versions. On 16 November 2026:

- Spring Boot **4.1.2** is GA and supports Java 17 through Java 27.
- The 4.1.2 BOM pins Spring Security **7.1.2**, Spring Framework **7.0.10**, Spring Session **4.1.1**.
- Spring Boot 4.2 is in RC and is **not** the production baseline.

**Correction:** pin `4.1.2` / `7.1.2`. Forbid snapshots and milestones.

### 1.2 Spring Security SAML is not an IdP

**Original:** use Spring Security 7 plus OpenSAML.

**Finding:** Spring Security 7 SAML support is **service-provider only** (`spring-security-saml2-service-provider`). Upstream documentation states Identity Provider support is out of scope. Spring Security 7 also dropped OpenSAML 4.

**Correction:** implement the IdP with **OpenSAML 5.2.3** directly. Use Spring Security only for application security (login, sessions, CSRF, headers, method security). Do not depend on `saml2Login` for IdP behaviour.

### 1.3 OpenSAML is not on Maven Central

**Finding:** OpenSAML artifacts are published to the Shibboleth Maven repository. Spring Security documents this requirement for its own SP module.

**Correction:** add `https://build.shibboleth.net/maven/releases/` as a repository, pin `opensaml-bom:5.2.3`, and import OpenSAML's full relevant stack (XML tooling, security, messaging, SAML API/impl). Using isolated OpenSAML classes without the parser pool and signature validators is a known source of XXE and wrapping bugs.

### 1.4 HTTP-Redirect is not a legal binding for SAML Responses

**Original:** support HTTP-Redirect and HTTP-POST for SSO.

**Finding:** SAML 2.0 Bindings forbid HTTP-Redirect for `<Response>` because of URL length and signature encoding. Spring Security SP also does not support Redirect for responses.

**Correction:**

| Message | Redirect | POST |
| ------- | -------- | ---- |
| AuthnRequest | Yes | Yes |
| Response / Assertion | **No** | **Yes** |
| LogoutRequest | Yes | Yes |
| LogoutResponse | Yes | Yes |

### 1.5 AES-256-GCM is not universally interoperable

**Original:** encrypt assertions with AES-256-GCM and RSA-OAEP.

**Finding:** XML Encryption 1.1 AES-GCM is **optional** in SAML conformance. Many SPs still decrypt only AES-128-CBC or AES-256-CBC, and many Java SPs default RSA-OAEP to SHA-1/MGF1-SHA1. Using GCM + OAEP-SHA-256 without advertising parameters in `EncryptedKey` will fail against conservative SPs.

**Correction:** default to AES-256-GCM + RSA-OAEP (SHA-256, MGF1-SHA256) with DigestMethod and MGF present in the message. Allow per-SP fallback to AES-128-CBC / AES-256-CBC and RSA-OAEP-MGF1P. Never enable RSA-1.5 key transport.

### 1.6 “Private keys never touch the Java heap” is not a true statement

**Finding:** SunPKCS11 still creates `PrivateKey` handles, session objects, and signature SPI instances in the JVM. Non-extractability is an HSM attribute (`CKA_EXTRACTABLE=false`, `CKA_SENSITIVE=true`), not a heap-absence guarantee.

**Correction:** promise **non-extractable token keys** and **no application-level key export**. Do not promise heap isolation.

### 1.7 Virtual threads plus PKCS#11

**Original:** enable `spring.threads.virtual.enabled=true`.

**Finding:** PKCS#11 is a native blocking API. Carrier-thread pinning is expected during `Signature.sign()`. That does not forbid virtual threads for HTTP, but signing must be isolated (bounded platform-thread executor or documented pinning).

**Correction:** virtual threads for request handling; PKCS#11 signing on a small platform-thread pool. Do not wrap every HSM call in an unbounded virtual-thread fan-out.

### 1.8 H2 is not a PostgreSQL substitute

**Original:** PostgreSQL production, H2 development.

**Finding:** Flyway scripts, advisory locks, `JSONB`, exclusion constraints, and replay uniqueness will diverge. `MODE=PostgreSQL` does not make H2 a protocol-state store.

**Correction:** H2 is allowed only for UI/dev of non-protocol screens. All security-sensitive tests and the default `dev` profile for SAML/MFA/session work use **Testcontainers PostgreSQL**. One Flyway tree: PostgreSQL.

### 1.9 SoftHSM2 is not a certified HSM

**Finding:** SoftHSM2 is a software PKCS#11 token. Token objects live on disk. Anyone with filesystem + PIN has the keys. It is the correct *interface* for later Thales/nShield/CloudHSM migration, not a compliance HSM.

**Correction:** treat SoftHSM2 as the **dev/lab and interface-compatible** backend. Document that production at assurance level “HSM” requires a physical or cloud HSM with the same PKCS#11 contract. Token directory permissions and SO-PIN protection are part of the security boundary.

### 1.10 `keytool -genkeypair` on PKCS#11 does not magically create a SAML signing certificate chain

**Finding:** Java can generate a keypair in the token and a self-signed certificate object. That certificate’s public key must match the token private key. Importing an unrelated PEM into SoftHSM as a certificate object without the matching private key will produce metadata that cannot sign.

**Correction:** generate the key **inside** the token, create the X.509 object from that public key (self-signed for lab; CSR to internal CA for production), store the certificate in the token *and* in the IdP signing-certificate table (public material only).

## 2. SAML protocol security gaps

| Gap | Risk | Required control |
| --- | ---- | ---------------- |
| No SAML metadata **signing** | Attackers can serve substitute IdP metadata | Sign IdP metadata with the active signing key; publish certificate fingerprints out of band |
| No signed **SP metadata import** path | Admin pastes a rogue ACS | Parse metadata with OpenSAML; verify signature if present; still require explicit ACS allow-list confirmation |
| `ForceAuthn` / `IsPassive` ignored | SSO reuse against policy | Honour `ForceAuthn` by stepping up; `IsPassive=true` must not show UI — return `NoPassive` |
| No `Comparison` / requested `AuthnContextClassRef` | SP asks for MFA, IdP asserts password | Map TOTP success to `urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken` (or configurable class); reject if Comparison=exact and IdP cannot satisfy |
| Persistent NameID without storage | Non-stable IDs break account linking | Pairwise persistent IDs stored per (user, SP) |
| Transient NameID reuse | Cross-session correlation | New transient ID per SSO session |
| No XML size / entity expansion budget | DoS, billion laughs | OpenSAML parser pool: no DTD, no external entities, max payload (default 256 KiB) |
| Request ID uniqueness only | Replay of signed LogoutRequest | Replay table for AuthnRequest, LogoutRequest, and assertion IDs we emit (to detect reflection) |
| Clock skew only on assertions | Stale AuthnRequests accepted | `NotOnOrAfter` / `IssueInstant` + skew on requests and logout |
| Destination checked, **Recipient** on SubjectConfirmationData not specified | Confused deputy ACS | Recipient must equal the allow-listed ACS used |
| No `InResponseTo` on unsolicited IdP-initiated SSO | Fine, but must omit rather than forge | IdP-initiated responses have no `InResponseTo` |
| Binding mismatch | Redirect request, POST ACS confusion | SP registry stores ACS **per binding** |
| Unsigned metadata consumed as trust | “Just import XML” | Trust is the admin-approved registry row, not the file |
| XML Signature Wrapping | Signed object ≠ used object | Validate signature on the protocol object that is later read; use ID-based references; never re-parse unsigned copies |
| No algorithm deny-list | SHA-1, RSA-1.5, HMAC-SHA1 | Explicit allow-list; reject anything else |
| SessionIndex missing on SLO | Logout of the wrong session | AuthnStatement.SessionIndex = IdP session index; SLO matches it |

## 3. Missing enterprise IdP requirements

Add these to the approved requirements:

1. **Administrator bootstrap / break-glass** — first-run setup with one-time token; emergency admin recovery with dual control and mandatory audit.
2. **Mandatory MFA for administrative roles.**
3. **Separation of duties** — `ADMIN`, `OPERATOR`, `AUDITOR`, `USER`. Auditors cannot reset MFA. Operators cannot change signing keys. Key rotation requires ADMIN + reauthentication.
4. **Consent / attribute release UI** — optional in v1, but the mapper must still default-deny attributes.
5. **SP metadata entityID uniqueness and ACS HTTPS requirement** in production.
6. **Certificate usage checks** — `digitalSignature` / `nonRepudiation` for signing; `keyEncipherment` / `dataEncipherment` for encryption; reject expired, not-yet-valid, or weak keys at registration.
7. **NTP / clock discipline** as an operational control. SAML is time-sensitive.
8. **Correlation IDs** on every request, copied into audit events.
9. **Rate limits** at account, source IP, and SP entity ID.
10. **Partial SLO semantics** documented to relying parties.
11. **Signed logout messages** policy per SP.
12. **NameID management** — persistent pairwise table, unlink, and regeneration under audit.
13. **Health vs readiness** — liveness: process up; readiness: DB + HSM sign self-test.
14. **Backup of SoftHSM token directory and SO-PIN** as a DR item, distinct from DB backup.
15. **Legal hold / audit retention** vs operational replay TTL.
16. **Content-Security-Policy** that still allows the SAML auto-POST form.
17. **Login CSRF** protection on the username/password form (in addition to session CSRF).
18. **Change-email** and **disable-account on password reset from admin** session-revocation rules.
19. **Device/browser** is *not* a second factor in v1 (TOTP only).
20. **SAML encryption certificate rollover** on the SP side, not only IdP signing rollover.

## 4. Authentication and session weaknesses in the original brief

| Issue | Correction |
| ----- | ---------- |
| “First-login MFA enrollment” without policy | Configurable: `required`, `optional`, `admin-enforced`. Default for users who access SPs: **required**. |
| Recovery codes without rate limit | Same brute-force counter as TOTP; codes are single-use hashed (Argon2id or HMAC with a distinct pepper). |
| MFA reset by admin without notify | Notify email (if verified) + revoke all sessions + audit. |
| Session fixation mentioned, but SAML POST may restore a session cookie | Regenerate session ID at password success **and** at MFA success. |
| Concurrent sessions | Configurable max; default 5 IdP sessions per user. Admin unlimited unless configured. |
| Cookie `SameSite=Strict` | **Forbidden** as default. Use `Lax` if only Redirect SSO is enabled; use `None; Secure` when HTTP-POST SSO/SLO is enabled. |
| Remember-me | **Not offered.** It bypasses MFA in too many implementations. |
| Spring Security `ROLE_MFA_PENDING` | Use a dedicated `Authentication` type (`MfaPendingAuthenticationToken`) with authority `MFA_PENDING`, not a real role stored on the user. |

## 5. HSM and certificate lifecycle gaps

1. SoftHSM default `CKA_EXTRACTABLE` is false for private keys — keep it false; **verify** with `pkcs11-tool --read-object` failure and a Java negative test.
2. `CKA_SENSITIVE=true` is required. The original “non-sensitive where supported” wording is dangerous. Sensitive means the value cannot be revealed in plaintext.
3. Key labels must be unique per generation (`idp-signing-YYYYMMDD-seq`). CKA_ID is the stable PKCS#11 identifier stored in the DB.
4. Public certificates **are** stored in PostgreSQL (and optionally as token certificate objects). That is public material.
5. Rotation needs a **quorum of SPs that have fetched new metadata** or a mandatory overlap window, not a hope that “SPs imported the cert”.
6. Never delete a token key in the same transaction as activation. Retire = stop signing, keep verifying historically if we ever needed to, then destroy after `retirement_grace`.
7. HSM PIN from env/secret manager; support PIN-file with `0600` only for lab.
8. Signing health check: sign a nonce, verify with published cert. Do not log the PIN or the nonce if it includes secrets (it should not).
9. Envelope encryption key for TOTP secrets is **not** in the HSM in v1 (optional later). It is a separate AES-256-GCM data key from a secret manager. Do not reuse the SAML signing key to wrap TOTP secrets — PKCS#11 RSA decrypt on a signing key is a key-misuse anti-pattern and may be disabled by attribute (`CKA_DECRYPT=false` on signing keys).

## 6. Database consistency issues

1. Failed-login counters must increment in a **row-level lock** or `UPDATE ... SET failures = failures + 1 RETURNING` to avoid lost updates across nodes.
2. Replay inserts use a unique constraint on `(message_id_hash, message_type)` and treat unique-violation as replay, not as a crash.
3. Session revocation must be transactional with logout audit.
4. Password change, TOTP enrollment, and recovery-code regeneration require reauthentication in the same request, not a previously established session alone.
5. Optimistic locking (`version`) on User, ServiceProvider, SigningKey.
6. Audit table is **append-only** at the application and DB grant level (`INSERT` only for the app role).
7. H2 schema generation via Hibernate is forbidden. Flyway only.

## 7. Scalability and HA

| Concern | v1 decision |
| ------- | ----------- |
| Sticky sessions | Not required if Spring Session JDBC + PostgreSQL protocol state |
| HSM | SoftHSM token dir is **not** multi-node safe on NFS without care. v1: one writer token store on local disk per environment, or a network HSM. Two app nodes may share a PKCS#11 network HSM; they must not share a SoftHSM directory over flaky FS. |
| Signing throughput | RSA-3072 on SoftHSM is CPU-bound. Size for peak SSO, not average. |
| Metadata | Cache generated metadata in memory, invalidate on key-state change. |
| Replay TTL | `max(request_max_age, assertion_lifetime) + 2 * clock_skew` |
| Connection pool | HikariCP sized for virtual threads (more connections than platform-thread era, still bounded) |

## 8. Operational and DR risks

1. Losing the SoftHSM token **and** having no backup means **all SPs must re-trust a new signing key**. Metadata rollover cannot help if both keys are gone.
2. Losing PostgreSQL without backup means users, persistent NameIDs, and sessions are gone. Persistent NameIDs cannot be reconstructed.
3. Restoring DB to a point before a key rotation, while the HSM has the new key, desynchronizes signing state. Backup **DB + HSM token + config** as one recovery unit.
4. PIN rotation requires application restart or a documented live-reload that does not log the PIN.
5. TLS private keys are independent of SAML keys. Mixing them in SoftHSM is allowed physically, forbidden by policy in v1.

## 9. Cryptographic correction (adopted)

Encryption and signing are different operations:

```
SAML Response
  └── Assertion
        ├── signed with IdP active signing private key (HSM, RSA-SHA256)
        └── optionally encrypted with:
              • data key: random AES-256-GCM (or SP-fallback CBC)
              • key transport: SP encryption certificate public key, RSA-OAEP
```

Never encrypt with a certificate from the AuthnRequest. Never sign with an SP key. Never decrypt TOTP secrets with the SAML signing key.

## 10. Wording that must be removed from future implementation prompts

- “private keys never leave / never touch the JVM”
- “Spring Security SAML IdP”
- “H2 is equivalent to PostgreSQL”
- “SameSite=Strict for all cookies”
- “global logout always logs the user out of every SP”
- “AES-256-GCM only”
- “Java 25 Virtual Threads make HSM calls cheap”

## 11. Outcome

The approved architecture in the following documents incorporates every correction above. Original requirements that remain valid are restated in [`spec/approved-requirements.md`](spec/approved-requirements.md).
