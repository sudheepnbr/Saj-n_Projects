# SAML interoperability matrix

Do not assume every Service Provider supports every option. Each SP registry row selects a profile from this matrix.

| Feature | v1 IdP behaviour | Default | Per-SP |
| ------- | ---------------- | ------- | ------ |
| AuthnRequest signing | Validate when required; ignore embedded untrusted certs | Required | Yes |
| Response signing | Always sign Response | Required | Policy may not disable in prod |
| Assertion signing | Always sign Assertion | Required | Same |
| Assertion encryption | Encrypt if SP flag + valid encryption cert | Off | Yes |
| Encryption algorithms | GCM default; CBC fallback | AES-256-GCM + RSA-OAEP-SHA256 | Yes |
| NameID persistent | Pairwise stored opaque | Available | Format selector |
| NameID email | `urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress` | Available | Yes |
| NameID transient | New per IdP session | Available | Yes |
| NameID unspecified | Mapped to username only if SP requests it | Discouraged | Yes |
| SSO Redirect | AuthnRequest in | On | Binding allow-list |
| SSO POST | AuthnRequest in, Response out | On | Binding allow-list |
| SSO Redirect Response | **Not implemented** (illegal / non-interop) | Off | No |
| SLO Redirect | LogoutRequest/Response | On | Yes |
| SLO POST | LogoutRequest/Response | On | Yes |
| SLO SOAP | Deferred | Off | — |
| Artifact binding | Deferred | Off | — |
| AuthnContext | Password vs TimeSyncToken | TimeSyncToken when MFA used | Configurable class refs |
| Comparison exact | Honour or fail | Honour | — |
| ForceAuthn | Re-challenge | Honour | — |
| IsPassive | No UI; NoPassive error | Honour | — |
| Clock skew | Applied to request and assertion | 120 s | Global |
| Assertion lifetime | Conditions + SubjectConfirmation | 300 s | SP override ≤ global max |
| SessionIndex | Always | On | — |
| Signed metadata | IdP metadata signed | On | — |
| WantAuthnRequestsSigned | Advertised in metadata | true | IdP default |

## Named SP classes (lab)

| SP | Notes |
| -- | ----- |
| Spring Security 7 SAML SP | HTTP-POST ACS; RSA-SHA256; good OpenSAML sibling |
| SimpleSAMLphp | Broad algorithm support; useful encryption tests |
| Shibboleth SP | Strict destination/audience; metadata-oriented |
| Keycloak as SAML client | Convenient UI; not a runtime dependency |
| FortiGate IPsec SAML (IKEv2+EAP) | Username from attribute `uid` (not NameID); ACS `/remote/saml/login`; see [`FORTIGATE.md`](../../../FORTIGATE.md) |

Each class is a Phase 6 test target, not a v1 code dependency.
