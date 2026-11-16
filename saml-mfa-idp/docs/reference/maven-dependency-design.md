# Maven dependency design (Phase 2 preview)

Do not add a `pom.xml` until Phase 2. This is the intended BOM-first design.

## Parent / BOM

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.1.2</version>
</parent>
```

Java: `<java.version>25</java.version>`

Properties:

```xml
<opensaml.version>5.2.3</opensaml.version>
```

## Repository (required)

```xml
<repository>
  <id>shibboleth-releases</id>
  <url>https://build.shibboleth.net/maven/releases/</url>
  <releases><enabled>true</enabled></releases>
  <snapshots><enabled>false</enabled></snapshots>
</repository>
```

## Production dependencies (managed unless noted)

- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-boot-starter-thymeleaf`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-boot-starter-jdbc` (session)
- `org.springframework.session:spring-session-jdbc`
- `org.flywaydb:flyway-core` + `flyway-database-postgresql`
- `org.postgresql:postgresql`
- `org.opensaml:opensaml-core-api` / `opensaml-core-impl`
- `org.opensaml:opensaml-saml-api` / `opensaml-saml-impl`
- `org.opensaml:opensaml-xmlsec-api` / `opensaml-xmlsec-impl`
- `org.opensaml:opensaml-security-api` / `opensaml-security-impl`
- `org.opensaml:opensaml-messaging-api` / `opensaml-messaging-impl`
- Bouncy Castle provider (Argon2id)
- ZXing core + javase (QR)
- JSON logging encoder (logstash-logback or equivalent GA)

Optional H2: `com.h2database:h2` with **runtime** scope, profile-gated, not on the SAML test classpath.

## Test

- `spring-boot-starter-test`
- `org.testcontainers:junit-jupiter`
- `org.testcontainers:postgresql`
- OpenSAML test utilities if available at 5.2.3
- SoftHSM present in CI image or documented skip

## Exclusions

- Do not import `spring-security-saml2-service-provider` into the IdP module.
- Do not use Keycloak libraries.
- Snapshot OpenSAML 5.3.x forbidden.

## Virtual threads

`application.yml`:

```yaml
spring.threads.virtual.enabled: true
```

Document PKCS#11 executor separately.
