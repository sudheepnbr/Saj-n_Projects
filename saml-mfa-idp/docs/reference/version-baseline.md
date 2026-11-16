# Version baseline

Verified **16 November 2026**. Production baseline uses only GA artifacts.

## Platform

| Component | Version | Source |
| --------- | ------- | ------ |
| Java | 25 (LTS) | OpenJDK / vendor TCK build |
| Maven | 3.9.x (≥ 3.6.3) | Spring Boot 4.1.2 system requirements |
| Spring Boot | **4.1.2** | Maven Central, released 22–23 Oct 2026 |
| Spring Framework | 7.0.10 | Boot 4.1.2 system requirements / BOM |
| Spring Security | **7.1.2** | `spring-security.version` in Boot 4.1.2 BOM |
| Spring Session | 4.1.1 | Boot 4.1.2 BOM |
| Spring Data BOM | 2026.0.x | Boot 4.1.2 |
| Tomcat | 11.0.27 | Boot 4.1.2 BOM |
| Hibernate ORM | 7.4.13.Final | Boot 4.1.2 BOM |
| Flyway | 12.4.0 | Boot 4.1.2 BOM |
| Thymeleaf | 3.1.5.RELEASE | Boot 4.1.2 BOM |
| PostgreSQL JDBC | 42.7.14 | Boot 4.1.2 BOM |
| H2 (non-default) | 2.4.240 | Boot 4.1.2 BOM |
| Testcontainers | 2.0.5 | Boot 4.1.2 BOM |
| Logback | 1.5.38 | Boot 4.1.2 BOM |
| OpenSAML | **5.2.3** | [Shibboleth Nexus](https://build.shibboleth.net/maven/releases/org/opensaml/opensaml-bom/5.2.3/) (19 Jun 2026) |
| Bouncy Castle | pin at implementation; required by Argon2id | Spring Security docs |
| SoftHSM | 2.6.x | Distro package |
| PostgreSQL server | 16 or 17 | Operations choice |
| ZXing | pin at implementation | QR codes |

Spring Boot 4.1.2 is compatible with Java 17 through **Java 27**. Java 25 is the chosen LTS runtime.

## Compatibility notes

- Spring Security 7.1.2 `Argon2PasswordEncoder` requires Bouncy Castle. Do not use `spring-security-crypto` in isolation without `spring-core` (standalone crypto 7.1.0/7.1.1 had a `StringUtils` issue; Boot-managed 7.1.2 is the pin).
- OpenSAML 5 requires Java 17+. 5.2.3 is the last published GA as of this review; 5.3.0 is SNAPSHOT — do not use.
- OpenSAML is **not** on Maven Central; add Shibboleth releases repo.
- Spring Security 7 removed OpenSAML 4 support in the SP module; irrelevant to IdP but confirms OpenSAML 5 is the ecosystem version.
- Virtual threads: `spring.threads.virtual.enabled=true` is supported; PKCS#11 signing is isolated.

## Forbidden

- Spring Boot 4.2 RC
- OpenSAML 5.3.0-SNAPSHOT
- Milestone Spring Security builds
- Unpinned `LATEST` / `RELEASE` Maven versions
