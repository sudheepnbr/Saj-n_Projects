#!/usr/bin/env bash
# Start the OIH IdP locally (Docker Compose preferred; Maven fallback).
set -euo pipefail
cd "$(dirname "$0")"

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  echo "Starting PostgreSQL + IdP with Docker Compose on http://localhost:8080"
  echo "First-run setup token: oih-local-setup"
  exec docker compose up --build "$@"
fi

echo "Docker Compose not found; using Maven + local PostgreSQL."
exec ./saml-mfa-idp/scripts/run-dev.sh
