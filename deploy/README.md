# Production deploy files

| File | Use |
| ---- | --- |
| `env.prod.example` | Copy to `../.env.prod` and fill secrets |
| `generate-secrets.sh` | Print unique secret values |
| `Caddyfile` | TLS reverse proxy used by `docker-compose.prod.yml` |

See [`../PRODUCTION.md`](../PRODUCTION.md).
