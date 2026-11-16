# ADR 0006 — Cryptographic key separation

## Status

Accepted

## Context

Signing, assertion encryption, TOTP protection, TLS, and database credentials have different lifecycles and compromise impacts.

## Decision

| Secret | Mechanism |
| ------ | --------- |
| SAML signing | HSM asymmetric private key |
| Assertion confidentiality | SP public encryption certificate + ephemeral AES key |
| TOTP secrets | AES-256-GCM envelope key from secret manager |
| Recovery codes | One-way hash |
| TLS | Separate server certificate |
| DB / HSM PIN | Secret manager |

Never reuse the SAML signing key for encryption or data wrapping.

## Consequences

- More secrets to rotate, but blast radius is contained.
- Signing keys can have `CKA_DECRYPT=false`.
