#!/usr/bin/env bash
set -euo pipefail
set +H

DEPLOY_DIR="/opt/restoos/deploy"
GITHUB_REPO="${RESTOOS_GITHUB_REPO:-AQUILA04/RestoOS}"
GITHUB_RAW="https://raw.githubusercontent.com/${GITHUB_REPO}/main/deploy"

ENV="${1:-}"
API_IMAGE="${2:-}"
FRONTEND_IMAGE="${3:-}"
shift 3

[[ -n "$ENV" && -n "$API_IMAGE" && -n "$FRONTEND_IMAGE" ]] || {
  echo "Usage: $0 <env> <api_image> <frontend_image> [--github-repo owner/repo]" >&2
  exit 1
}

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --github-repo)
      GITHUB_REPO="$2"
      GITHUB_RAW="https://raw.githubusercontent.com/${GITHUB_REPO}/main/deploy"
      shift
      ;;
    --env-file-content)
      ENV_FILE_CONTENT="$2"
      shift
      ;;
  esac
  shift
done

mkdir -p /opt/restoos
curl -sSL "$GITHUB_RAW/update-deploy.sh" -o /opt/restoos/update-deploy.sh
chmod +x /opt/restoos/update-deploy.sh
export RESTOOS_GITHUB_REPO="$GITHUB_REPO"
/opt/restoos/update-deploy.sh

chmod +x "$DEPLOY_DIR"/*.sh
"$DEPLOY_DIR/setup-server.sh"

STACK_ENV="/opt/restoos/$ENV/.env"
mkdir -p "/opt/restoos/$ENV"
if [[ -n "${ENV_FILE_CONTENT:-}" ]]; then
  printf '%s\n' "$ENV_FILE_CONTENT" > "$STACK_ENV"
elif [[ ! -f "$STACK_ENV" ]]; then
  cp "$DEPLOY_DIR/.env.prod.example" "$STACK_ENV"
fi

upsert_env() {
  local key="$1"
  local value="$2"
  local file="$3"
  if grep -q "^${key}=" "$file" 2>/dev/null; then
    sed -i "s#^${key}=.*#${key}=${value}#" "$file"
  else
    printf '%s=%s\n' "$key" "$value" >> "$file"
  fi
}

upsert_env "API_IMAGE" "$API_IMAGE" "$STACK_ENV"
upsert_env "FRONTEND_IMAGE" "$FRONTEND_IMAGE" "$STACK_ENV"

"$DEPLOY_DIR/deploy.sh" "$ENV"
