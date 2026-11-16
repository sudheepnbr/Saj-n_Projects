# Security test matrix

Expected outcomes for Phase 5–7 automated tests.

| ID | Case | Setup | Expected |
| -- | ---- | ----- | -------- |
| AUTH-01 | Bad password | Valid username | Generic error, `LOGIN_FAILURE`, no session |
| AUTH-02 | Unknown user | Random username | Same generic error, dummy hash path |
| AUTH-03 | Lockout | N failures | Account locked, audit `LOCKOUT`, other node also locked |
| AUTH-04 | Timing | Unknown vs bad password | No user-not-found message |
| MFA-01 | Skip MFA | Password only, call SSO | No assertion, 401/redirect to MFA |
| MFA-02 | TOTP replay | Same code twice | Second rejected |
| MFA-03 | Recovery reuse | Used code | Rejected |
| MFA-04 | Enrollment without confirm | Secret generated, no TOTP | `mfa_enabled` still false |
| SAML-01 | Happy Redirect SSO | Registered SP, MFA done | POST Response, signed, audience=SP |
| SAML-02 | Happy POST SSO | Same | Same |
| SAML-03 | Unsigned request | SP wants signed | Reject, `SAML_SIG_FAIL` |
| SAML-04 | Bad ACS | Attacker URL | Reject, no assertion |
| SAML-05 | Unregistered entityID | Random issuer | Reject |
| SAML-06 | Destination mismatch | Wrong SSO URL | Reject |
| SAML-07 | Replay AuthnRequest | Same ID twice, two nodes | Second reject |
| SAML-08 | Stale IssueInstant | Older than max age | Reject |
| SAML-09 | Algorithm SHA-1 | Signed with RSA-SHA1 | Reject |
| SAML-10 | Embedded attacker cert | Signed by unknown key | Reject |
| SAML-11 | XXE | External entity in XML | Reject, no outbound fetch |
| SAML-12 | XSW | Signature over different node | Reject |
| SAML-13 | Oversized XML | >256 KiB | 400 |
| SAML-14 | IdP-initiated | Authenticated user | Unsolicited Response, no InResponseTo |
| SAML-15 | Encryption | SP encrypt=true | EncryptedAssertion, SP test key decrypts |
| SAML-16 | Encryption untrusted cert | Cert only in request | Still uses registry cert or fail closed |
| SAML-17 | Mapper deny | Field not in mapping | Attribute absent |
| SAML-18 | ForceAuthn | Existing SSO session | Re-auth |
| SAML-19 | IsPassive no session | — | NoPassive, no HTML login |
| CSRF-01 | Password change | No token | 403 |
| CSRF-02 | SAML POST SSO | No CSRF token | Processed if SAML valid |
| SESS-01 | Fixation | Cookie before login | Cookie value changes after MFA |
| SESS-02 | Revoke | Admin revoke | Further /account 401; no new assertion |
| SESS-03 | SameSite | POST binding on | Cookie not Strict |
| SLO-01 | SP-initiated | Valid LogoutRequest | Session gone, LogoutResponse signed |
| SLO-02 | Replay logout | Same ID | Reject |
| SLO-03 | Partial | Second SP timeout | `SLO_PARTIAL` audit |
| ADMIN-01 | User hits /admin | USER role | 403 |
| ADMIN-02 | YAML override prod | File vs DB ACS | DB wins, audit |
| KEY-01 | Extractable key | Test token misconfigured | Startup/readiness fail |
| KEY-02 | Rotation | NEXT published | Metadata has 2 certs; signatures from ACTIVE |
| KEY-03 | Sign with wrap key | Attempt | Impossible by design / test SigningService interface |
| HDR-01 | Security headers | HTML response | CSP, HSTS (prod), nosniff, frame deny |
| SEC-01 | Actuator | Unauthenticated /actuator | Denied except liveness if explicitly allowed |
| SEC-02 | Stack trace | Force error in prod | Generic page |

Audience mismatch and expired assertion **at the SP** are verified in the independent SP lab (the IdP’s job is to emit correct values; SAML-01 checks emission).
