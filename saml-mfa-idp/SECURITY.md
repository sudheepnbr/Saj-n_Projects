# Security policy

This repository contains the **InfraMindTech Identity Provider** (local-identity SAML 2.0). Architecture lives on `main`; the runnable runtime is on branch `cursor/saml-mfa-idp-phases-3-7-04b9`.

## Reporting a vulnerability

Do not open a public issue for suspected vulnerabilities.

Email the repository owner with:

- a description of the issue
- affected design section or, later, module and version
- reproduction steps or a threat narrative
- impact assessment (confidentiality, integrity, availability, authentication bypass)

Do not attach private keys, HSM PINs, password hashes, TOTP secrets, recovery codes, or live SAML assertions.

## Non-goals for reports

The following are expected by design and are not vulnerabilities unless they can be exploited:

- SAML HTTP-POST endpoints that omit CSRF tokens, provided they still enforce destination, issuer, signature, ACS allow-list, and replay checks
- SameSite cookie policy other than `Strict` when required for SAML POST bindings
- Best-effort Single Logout against unreachable Service Providers

## Secrets that must never be committed

- HSM user PIN and SO PIN
- database credentials
- TOTP envelope-encryption keys
- TLS private keys
- bootstrap administrator passwords
- application session secrets
