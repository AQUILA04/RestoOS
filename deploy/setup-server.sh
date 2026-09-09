#!/usr/bin/env bash
set -euo pipefail
set +H

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="/opt/restoos"

echo "=== RestoOS Server Setup ==="

mkdir -p "$ROOT/prod/releases" "$ROOT/deploy"
if [[ "$DEPLOY_DIR" != "$ROOT/deploy" ]]; then
  cp -a "$DEPLOY_DIR"/. "$ROOT/deploy/"
  chmod +x "$ROOT/deploy"/*.sh 2>/dev/null || true
fi

for net in traefik-public optimizesolux-common; do
  docker network inspect "$net" > /dev/null 2>&1 || docker network create "$net"
done

PROD_ENV="$ROOT/prod/.env"
if [[ ! -f "$PROD_ENV" ]]; then
  cp "$ROOT/deploy/.env.prod.example" "$PROD_ENV"
  chmod 600 "$PROD_ENV"
  echo "Created $PROD_ENV"
else
  echo "$PROD_ENV already exists, leaving untouched."
fi

echo "Next:"
echo "docker compose -f $ROOT/deploy/docker-compose.prod.yml --project-name restoos-prod --env-file $PROD_ENV up -d"
