# ADR 0009 — Version baseline

## Status

Accepted

## Context

The brief asked for Java 25, Spring Boot 4.1, Spring Security 7, OpenSAML 5 without pinning patches. Boot 4.2 is already in RC as of late 2026.

## Decision

Pin GA versions verified 16 November 2026:

- Java 25
- Spring Boot 4.1.2
- Spring Security 7.1.2 (BOM)
- OpenSAML 5.2.3
- related BOM versions in `docs/reference/version-baseline.md`

No snapshots, milestones, or RCs in production.

## Consequences

- Patch upgrades (4.1.2 → 4.1.x) are allowed after changelog review.
- Minor upgrades (4.2) require a new ADR.
