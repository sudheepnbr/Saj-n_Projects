# ADR 0008 — CSRF and SAML bindings

## Status

Accepted

## Context

SAML HTTP-POST AuthnRequest and LogoutRequest are cross-origin POSTs without CSRF tokens. Disabling CSRF globally would expose `/account` and `/admin`. `SameSite=Strict` cookies break SAML POST.

## Decision

- CSRF enabled globally.
- CSRF ignored only for `POST /saml2/idp/sso` and `POST /saml2/idp/slo`.
- Those endpoints compensate with SAML validation (destination, issuer, ACS allow-list, signature policy, replay, size).
- Cookie SameSite=`None; Secure` when POST bindings are enabled, otherwise `Lax`.
- Responses always use HTTP-POST, never Redirect.

## Consequences

- CSP `form-action` must list registered ACS origins.
- Login forms still use CSRF tokens.
