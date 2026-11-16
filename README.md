# Saj@n Projects

Personal project workspace for [sudheepnbr](https://github.com/sudheepnbr).

All work for these projects stays in this repository.

## Projects

| Path | Project | Status |
| ---- | ------- | ------ |
| [`saml-mfa-idp/`](saml-mfa-idp/) | Production-grade **local-identity SAML 2.0 Identity Provider** (Java 25, Spring Boot 4.1, SoftHSM2, TOTP MFA, SAML SSO/SLO) | Phases 1–7 |

## Run it

Follow **[`START.md`](START.md)**. Short version:

```bash
git clone -b cursor/saml-mfa-idp-phases-3-7-04b9 https://github.com/sudheepnbr/Saj-n_Projects.git
cd Saj-n_Projects
./run.sh
```

Then open http://localhost:8080/setup with token `oih-local-setup`.

**Production** (HTTPS, PKCS#11, unique secrets): [`PRODUCTION.md`](PRODUCTION.md) and [`docs/OIH-IdP-Setup-Instructions.pdf`](docs/OIH-IdP-Setup-Instructions.pdf).

## SAML MFA Identity Provider

Start at [`START.md`](START.md), [`saml-mfa-idp/README.md`](saml-mfa-idp/README.md), and [`saml-mfa-idp/docs/README.md`](saml-mfa-idp/docs/README.md).

Phase 1 architecture is on `main`. Later phases add identity, HSM signing, TOTP, SAML SSO/SLO, and production operations under `saml-mfa-idp/`.

## License

MIT. See [LICENSE](LICENSE).
