# Architecture Decision Records

| ADR | Title | Status |
| --- | ----- | ------ |
| [0001](0001-modular-monolith.md) | Modular monolith, not microservices | Accepted |
| [0002](0002-opensaml-idp.md) | OpenSAML 5 IdP, Spring Security for app security only | Accepted |
| [0003](0003-pkcs11-hsm-abstraction.md) | PKCS#11 HSM abstraction with SoftHSM2 | Accepted |
| [0004](0004-postgresql-authoritative-state.md) | PostgreSQL is the state store | Accepted |
| [0005](0005-local-identity-only.md) | No external IdP, LDAP, or brokering | Accepted |
| [0006](0006-crypto-key-separation.md) | Separate signing, encryption, wrap, and TLS keys | Accepted |
| [0007](0007-authentication-states.md) | MFA-pending is not fully authenticated | Accepted |
| [0008](0008-csrf-and-saml-bindings.md) | CSRF except compensated SAML POST | Accepted |
| [0009](0009-version-baseline.md) | Pinned GA versions | Accepted |
| [0010](0010-encryption-interop.md) | Encryption algorithm policy | Accepted |
| [0011](0011-xml-security-runtime.md) | Santuario XML security runtime; OpenSAML remains the architecture pin | Accepted |
