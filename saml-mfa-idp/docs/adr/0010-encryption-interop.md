# ADR 0010 — Assertion encryption interoperability

## Status

Accepted

## Context

AES-256-GCM and RSA-OAEP-SHA256 are the right modern default but are optional in SAML and under-supported by some SPs. RSA-1.5 is forbidden. SHA-1 OAEP is common in older Java SPs.

## Decision

- Default: AES-256-GCM + RSA-OAEP with SHA-256 and MGF1-SHA256, parameters present in `EncryptedKey`.
- Per-SP override: AES-128-CBC or AES-256-CBC; RSA-OAEP-MGF1P.
- Encryption uses the registered SP encryption certificate only.
- If encryption is required and no valid SP encryption cert exists, do not issue.

## Consequences

- Admin UI must capture encryption algorithm preference per SP.
- Interop tests cover at least GCM and one CBC SP.
