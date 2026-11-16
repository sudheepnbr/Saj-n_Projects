# ADR 0005 — Local identity only

## Status

Accepted

## Context

The product is an on-premises IdP with locally managed users. Brokering to Entra, LDAP, or social login would change the threat model, session design, and operational ownership.

## Decision

v1 stores and authenticates identities only in this application. No LDAP, Active Directory, OAuth social login, SAML IdP proxying, or Keycloak runtime.

## Consequences

- Administrators provision users.
- No “login with Microsoft” in this codebase.
- A FortiGate-Entra broker is a **different product** and must not be mixed into this architecture.
