# ADR 0001 — Modular monolith

## Status

Accepted

## Context

An IdP must keep authentication, session, replay, and signing consistent. Splitting those into microservices in v1 adds distributed transactions without operational staff to run them.

## Decision

Ship one Spring Boot application with package-level modules and interfaces. PostgreSQL is shared. PKCS#11 is a library call, not a service.

## Consequences

- Simple deployment and backup unit.
- Module boundaries must be enforced by reviews, not by network.
- A future extraction of the SAML module is possible because OpenSAML usage is isolated.
