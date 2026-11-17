# Saj@n Projects

Personal project workspace for [sudheepnbr](https://github.com/sudheepnbr).

All work for these projects stays in this repository.

## Projects

| Path | Project | Status |
| ---- | ------- | ------ |
| [`saml-mfa-idp/`](saml-mfa-idp/) | **InfraMindTech** Identity Provider — local-identity SAML 2.0 IdP (Java, Spring Boot, SoftHSM2, TOTP MFA, SAML SSO/SLO) | Phases 1–7 complete |

## InfraMindTech Identity Provider

Start at [`START.md`](START.md), [`saml-mfa-idp/README.md`](saml-mfa-idp/README.md), and [`saml-mfa-idp/docs/README.md`](saml-mfa-idp/docs/README.md).

| Phase | Deliverable | Status |
| ----- | ----------- | ------ |
| **1** | Architecture, threat model, ADRs, roadmap | Complete |
| **2** | Users, passwords, RBAC, admin console | Complete |
| **3** | PKCS#11 / SoftHSM2 signing and rotation | Complete |
| **4** | TOTP, recovery codes, SSO sessions | Complete |
| **5** | SAML IdP, metadata, assertion issuance | Complete |
| **6** | SLO and SP interoperability | Complete |
| **7** | HA, monitoring, backup, production cutover | Complete |

### Run it

```bash
git clone -b cursor/saml-mfa-idp-phases-3-7-04b9 https://github.com/sudheepnbr/Saj-n_Projects.git
cd Saj-n_Projects
./run.sh
```

Then open http://localhost:8080/setup with token `oih-local-setup`.

**Production** (HTTPS, PKCS#11, unique secrets): [`PRODUCTION.md`](PRODUCTION.md) and [`docs/IMT-IdP-Setup-Instructions.pdf`](docs/IMT-IdP-Setup-Instructions.pdf).

## License

MIT. See [LICENSE](LICENSE).
