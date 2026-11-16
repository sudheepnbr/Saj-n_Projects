#!/usr/bin/env bash
# Run the IdP with Maven against local PostgreSQL (no Docker).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! command -v java >/dev/null 2>&1; then
  echo "Java 21+ is required." >&2
  exit 1
fi
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven 3.9+ is required." >&2
  exit 1
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://127.0.0.1:5432/oih_idp}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-oih}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-oih_dev_only}"
export OIH_SETUP_TOKEN="${OIH_SETUP_TOKEN:-oih-local-setup}"
export OIH_ENTITY_ID="${OIH_ENTITY_ID:-http://localhost:8080/saml2/idp/metadata}"
export OIH_BASE_URL="${OIH_BASE_URL:-http://localhost:8080}"
export OIH_TOTP_WRAP_KEY="${OIH_TOTP_WRAP_KEY:-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=}"

if PGPASSWORD="${SPRING_DATASOURCE_PASSWORD}" psql -h 127.0.0.1 -U "${SPRING_DATASOURCE_USERNAME}" -d oih_idp -c 'SELECT 1' >/dev/null 2>&1; then
  echo "PostgreSQL database oih_idp is reachable."
else
  cat >&2 <<'EOF'
Cannot reach PostgreSQL database oih_idp as user oih.

As a Postgres superuser run:

  CREATE USER oih WITH PASSWORD 'oih_dev_only';
  CREATE DATABASE oih_idp OWNER oih;

Then start this script again. Or install Docker and run ./run.sh from the repo root.
EOF
  exit 1
fi

echo "IdP will listen on http://localhost:8080"
echo "Open http://localhost:8080/setup  — token: ${OIH_SETUP_TOKEN}"
exec mvn spring-boot:run -Dspring-boot.run.profiles=dev
