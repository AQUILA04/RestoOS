#!/usr/bin/env bash
set -euo pipefail
set +H

ENV="${1:-prod}"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
STACK_DIR="/opt/restoos/$ENV"
ENV_FILE="$STACK_DIR/.env"
COMPOSE_FILE="$ROOT_DIR/docker-compose.$ENV.yml"
PROJECT_NAME="restoos-$ENV"

[[ -f "$ENV_FILE" ]] || { echo "Missing env file: $ENV_FILE" >&2; exit 1; }

set -a
source "$ENV_FILE"
set +a

echo ">>> [deploy] Logging in to GHCR if credentials exist..."
if [[ -n "${GHCR_USERNAME:-}" && -n "${GHCR_TOKEN:-}" ]]; then
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
fi

echo ">>> [deploy] Pulling images..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull || true

echo ">>> [deploy] Starting stack..."
docker compose -f "$COMPOSE_FILE" --project-name "$PROJECT_NAME" --env-file "$ENV_FILE" up -d

echo ">>> [deploy] Smoke checks..."
docker compose -f "$COMPOSE_FILE" --project-name "$PROJECT_NAME" --env-file "$ENV_FILE" ps

APP_HOST="${APP_HOSTNAME:-restoos.optimizesolux.com}"
SMOKE_URL="https://${APP_HOST}/api/v1/auth/signup"
echo ">>> [deploy] API routing smoke: POST ${SMOKE_URL}"

# Wait briefly for Traefik to pick up new labels / nginx reload.
sleep 5

SMOKE_TMP="$(mktemp)"
SMOKE_CODE="$(curl -sS -o "$SMOKE_TMP" -w "%{http_code}" \
  -X POST "$SMOKE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{}' \
  --max-time 30 || echo "000")"
SMOKE_BODY="$(head -c 400 "$SMOKE_TMP" || true)"
rm -f "$SMOKE_TMP"

echo ">>> [deploy] Smoke HTTP ${SMOKE_CODE}"
echo ">>> [deploy] Smoke body (truncated): ${SMOKE_BODY}"

# Fail if nginx SPA caught the request (405 HTML) or non-JSON HTML.
if [[ "$SMOKE_CODE" == "405" ]]; then
  echo "ERROR: POST /api/v1/auth/signup returned 405 — Traefik/nginx is routing /api to the SPA, not Spring." >&2
  exit 1
fi
if echo "$SMOKE_BODY" | grep -qiE '<!DOCTYPE|<html|nginx'; then
  echo "ERROR: POST /api/v1/auth/signup returned HTML — /api is not reaching the backend." >&2
  exit 1
fi
if [[ "$SMOKE_CODE" == "000" ]]; then
  echo "ERROR: Smoke request failed to connect to ${SMOKE_URL}" >&2
  exit 1
fi

# Expected: Spring JSON 4xx (e.g. 400 validation) — proves backend routing works.
echo ">>> [deploy] API routing smoke OK (backend responded, not SPA HTML)"
