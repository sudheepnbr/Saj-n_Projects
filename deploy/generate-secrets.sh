#!/usr/bin/env bash
# Print unique production secret values. Redirect into .env.prod and edit hostnames.
set -euo pipefail
if ! command -v openssl >/dev/null; then
  echo "openssl is required" >&2
  exit 1
fi
DB_PASSWORD="$(openssl rand -base64 18)"
cat <<EOF
# Generated $(date -u +%Y-%m-%dT%H:%MZ). Edit OIH_PUBLIC_HOST / entity ID / base URL.
OIH_PUBLIC_HOST=idp.example.com
OIH_ENTITY_ID=https://idp.example.com/saml2/idp/metadata
OIH_BASE_URL=https://idp.example.com
OIH_SETUP_TOKEN=$(openssl rand -base64 24)
OIH_TOTP_WRAP_KEY=$(openssl rand -base64 32)
OIH_HSM_PIN=$(openssl rand -base64 12)
OIH_HSM_SO_PIN=$(openssl rand -base64 12)
OIH_HSM_LIBRARY=/usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so
OIH_HSM_SLOT_LIST_INDEX=0
POSTGRES_USER=oih
POSTGRES_PASSWORD=${DB_PASSWORD}
POSTGRES_DB=oih_idp
SPRING_DATASOURCE_USERNAME=oih
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
EOF
echo "Store this file in a secret manager. Do not commit it." >&2
