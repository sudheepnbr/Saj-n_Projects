#!/usr/bin/env bash
# Initialize SoftHSM on first boot, then start the IdP as a non-root user.
set -euo pipefail

TOKEN_DIR="${SOFTHSM_TOKEN_DIR:-/var/lib/softhsm/tokens}"
export SOFTHSM2_CONF="${SOFTHSM2_CONF:-/etc/softhsm/softhsm2.conf}"
mkdir -p "$TOKEN_DIR"

if [[ -z "${OIH_HSM_PIN:-}" ]]; then
  echo "OIH_HSM_PIN is required in production." >&2
  exit 1
fi
if [[ -z "${OIH_HSM_SO_PIN:-}" ]]; then
  echo "OIH_HSM_SO_PIN is required to initialize the PKCS#11 token." >&2
  exit 1
fi

if ! softhsm2-util --show-slots 2>/dev/null | grep -q "oih-idp"; then
  echo "Initializing SoftHSM token label=oih-idp"
  softhsm2-util --init-token --free --label "oih-idp" --so-pin "$OIH_HSM_SO_PIN" --pin "$OIH_HSM_PIN"
fi

chown -R idp:idp "$TOKEN_DIR" 2>/dev/null || true
exec runuser -u idp -- java -jar /app/idp.jar
