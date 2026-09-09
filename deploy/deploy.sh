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
