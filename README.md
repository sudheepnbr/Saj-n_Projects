# Saj@n Projects

Personal project workspace for [sudheepnbr](https://github.com/sudheepnbr).

All work for these projects stays in this repository.

## Projects

| Path | Project | Status |
| ---- | ------- | ------ |
| [`saml-mfa-idp/`](saml-mfa-idp/) | Production-grade **local-identity SAML 2.0 Identity Provider** (Java 25, Spring Boot 4.1, OpenSAML 5, SoftHSM2, TOTP MFA) | Phase 2 — core identity platform |

## SAML MFA Identity Provider

Start at [`saml-mfa-idp/README.md`](saml-mfa-idp/README.md) and [`saml-mfa-idp/docs/README.md`](saml-mfa-idp/docs/README.md).

Phase 1 architecture is on `main`. Phase 2 adds the runnable identity platform (users, Argon2id, lockout, admin/account consoles) under `saml-mfa-idp/`. SAML, HSM, and TOTP are later phases.

## License

MIT. See [LICENSE](LICENSE).
