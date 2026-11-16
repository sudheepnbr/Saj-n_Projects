#!/usr/bin/env bash
# Build OIH-Local-Identity-Provider-full-stack.zip (source + docs, no .git / target).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STAGE="$(mktemp -d)"
NAME="OIH-Local-Identity-Provider"
DEST="${1:-$ROOT/dist/${NAME}-full-stack.zip}"
mkdir -p "$(dirname "$DEST")"

mkdir -p "$STAGE/$NAME"
tar -C "$ROOT" --exclude='.git' --exclude='target' --exclude='dist' \
  --exclude='.env.prod' --exclude='.idea' \
  -cf - . | tar -C "$STAGE/$NAME" -xf -

# Keep scripts executable inside the archive
chmod +x "$STAGE/$NAME/run.sh" \
  "$STAGE/$NAME/deploy/generate-secrets.sh" \
  "$STAGE/$NAME/saml-mfa-idp/scripts/"*.sh 2>/dev/null || true

rm -f "$DEST"
(cd "$STAGE" && zip -qr "$DEST" "$NAME")
rm -rf "$STAGE"
echo "$DEST"
ls -lh "$DEST"
