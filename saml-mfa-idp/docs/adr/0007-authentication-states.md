# ADR 0007 — Authentication states

## Status

Accepted

## Context

If an MFA-pending SecurityContext is treated as authenticated, SAML assertions can be issued after password-only proof.

## Decision

Introduce distinct authentication types:

- `MfaPendingAuthenticationToken`
- `MfaEnrollmentAuthenticationToken`
- fully authenticated token with real roles

SAML issuance and `/account` (except enrollment) require fully authenticated. Pending tokens are restricted by the security matcher **and** by `SsoSessionService.requireFullyAuthenticated()`.

Session IDs are regenerated on each step-up.

## Consequences

- Slightly more Spring Security configuration.
- MFA bypass becomes a testable invariant rather than a filter-order accident.
