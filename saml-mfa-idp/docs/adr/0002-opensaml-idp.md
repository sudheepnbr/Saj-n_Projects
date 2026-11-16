# ADR 0002 — OpenSAML 5 for IdP protocol

## Status

Accepted

## Context

Spring Security 7 documents SAML **service provider** support only and states that migrating a Spring Security SAML Extensions IdP is out of scope. OpenSAML 5.2.3 is the maintained Java SAML stack (Apache 2.0), used by Shibboleth IdP.

## Decision

Implement SAML 2.0 IdP behaviour with OpenSAML 5.2.3 (parser pool, metadata objects, signature validation, encryption). Use Spring Security 7.1.2 for servlet security, form login, CSRF, headers, and method security.

Do not use `spring-security-saml2-service-provider` as the IdP.

A Spring Security **SP** test application is used in Phase 6 as an independent relying party.

## Consequences

- Must add the Shibboleth Maven repository.
- Team must follow OpenSAML initialization (`InitializationService`) and parser-pool guidance.
- No invented XML.
