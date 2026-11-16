# Package structure (Phase 2 preview)

Not compiled in Phase 1. This is the intended Maven module layout: **one** Maven module unless size later justifies a `idp-crypto` split.

```
com.oih.idp
├── IdpApplication.java
├── config
│   ├── IdpProperties.java
│   ├── SecurityChainsConfig.java
│   ├── OpenSamlConfig.java
│   ├── Pkcs11Config.java
│   └── PersistenceConfig.java
├── identity
│   ├── domain
│   ├── repo
│   └── UserService.java
├── policy
│   ├── PasswordPolicyService.java
│   ├── LockoutService.java
│   └── BreachedPasswordService.java
├── mfa
│   ├── TotpService.java
│   ├── RecoveryCodeService.java
│   └── EnvelopeCrypto.java
├── session
│   ├── SsoSessionService.java
│   ├── MfaPendingAuthenticationToken.java
│   └── SessionRevocationService.java
├── saml
│   ├── web
│   │   ├── MetadataController.java
│   │   ├── SsoController.java
│   │   └── SloController.java
│   ├── AuthnRequestValidator.java
│   ├── AssertionFactory.java
│   ├── SamlMessageWriter.java
│   └── ReplayGuard.java
├── sp
│   ├── ServiceProviderRegistry.java
│   └── SpCertificateValidator.java
├── mapping
│   └── AttributeMapper.java
├── crypto
│   ├── SigningService.java
│   ├── Pkcs11SigningService.java
│   └── AssertionEncryptionService.java
├── keys
│   └── KeyLifecycleService.java
├── audit
│   └── AuditService.java
├── account
│   └── web
├── admin
│   └── web
└── web
    └── security
```

Templates: `src/main/resources/templates/{login,mfa,account,admin,saml}/`

Flyway: `src/main/resources/db/migration/V1__...sql`
