# Saj@n Projects

Personal project workspace for [sudheepnbr](https://github.com/sudheepnbr).

All work for these projects stays in this repository.

## Projects

| Path | Project | Status |
| ---- | ------- | ------ |
| [`saml-mfa-idp/`](saml-mfa-idp/) | **InfraMindTech** Identity Provider — local-identity SAML 2.0 IdP (Java, Spring Boot, SoftHSM2, TOTP MFA, SAML SSO/SLO) | Phases 1–7 complete |

## InfraMindTech Identity Provider

| Phase | Deliverable | Status |
| ----- | ----------- | ------ |
| **1** | Architecture, threat model, ADRs, roadmap | Complete |
| **2** | Users, passwords, RBAC, admin console | Complete |
| **3** | PKCS#11 / SoftHSM2 signing and rotation | Complete |
| **4** | TOTP, recovery codes, SSO sessions | Complete |
| **5** | SAML IdP, metadata, assertion issuance | Complete |
| **6** | SLO and SP interoperability | Complete |
| **7** | HA, monitoring, backup, production cutover | Complete |

Architecture docs: [`saml-mfa-idp/README.md`](saml-mfa-idp/README.md) · [`saml-mfa-idp/docs/README.md`](saml-mfa-idp/docs/README.md)

The runnable stack (UI, Docker, SAML IdP) is on branch [`cursor/saml-mfa-idp-phases-3-7-04b9`](https://github.com/sudheepnbr/Saj-n_Projects/tree/cursor/saml-mfa-idp-phases-3-7-04b9) ([PR #2](https://github.com/sudheepnbr/Saj-n_Projects/pull/2)).

```bash
git clone -b cursor/saml-mfa-idp-phases-3-7-04b9 https://github.com/sudheepnbr/Saj-n_Projects.git
cd Saj-n_Projects
./run.sh
```

Then open http://localhost:8080/setup with token `oih-local-setup`.

## License

MIT. See [LICENSE](LICENSE).
