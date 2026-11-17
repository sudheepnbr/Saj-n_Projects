#!/usr/bin/env bash
# Wipe local IdP data (users, sessions, SPs) so /setup works again.
# Keeps Flyway history and seeded roles. Restart the IdP afterwards.
set -euo pipefail
PGHOST="${PGHOST:-127.0.0.1}"
PGUSER="${PGUSER:-oih}"
PGPASSWORD="${PGPASSWORD:-oih_dev_only}"
PGDATABASE="${PGDATABASE:-oih_idp}"
export PGPASSWORD

psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" <<'SQL'
TRUNCATE password_history, user_roles, users, auth_source_throttle,
         audit_events, admin_bootstrap, mfa_secrets, recovery_codes,
         idp_sessions, sp_sessions, logout_correlation, replay_cache,
         persistent_nameids, attribute_mappings, sp_certificates, sp_acs,
         sp_slo, service_providers, SPRING_SESSION_ATTRIBUTES, SPRING_SESSION
         RESTART IDENTITY CASCADE;
SQL

echo "Database ${PGDATABASE} is empty. Restart the IdP, then open http://localhost:8080/setup"
echo "Local setup token: ${OIH_SETUP_TOKEN:-oih-local-setup}"
