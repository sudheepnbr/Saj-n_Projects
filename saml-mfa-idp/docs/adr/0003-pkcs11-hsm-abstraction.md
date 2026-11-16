# ADR 0003 — PKCS#11 HSM abstraction

## Status

Accepted

## Context

SAML signing keys must not live in JKS/PKCS12 in the application. SoftHSM2 provides a PKCS#11 token for development and as a stand-in for hardware HSMs.

## Decision

- Generate signing private keys in the token.
- `CKA_EXTRACTABLE=false`, `CKA_SENSITIVE=true`, `CKA_SIGN=true`, `CKA_DECRYPT=false`.
- Access via Java SunPKCS11 and a `SigningService` interface.
- PIN from the environment or a secret manager.
- Verify non-extractability at startup.
- Do not claim keys never exist as JVM `PrivateKey` handles.

## Consequences

- SoftHSM file token is a secret and is not multi-node safe on NFS.
- Hardware HSM migration is a configuration change plus operational work, not a redesign.
- Signing runs on a bounded platform-thread pool because PKCS#11 pins virtual carrier threads.
