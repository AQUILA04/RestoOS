#!/usr/bin/env bash
set -euo pipefail

REPO="${RESTOOS_GITHUB_REPO:-AQUILA04/RestoOS}"
ROOT="/opt/restoos"

echo ">>> [update-deploy] Fetching latest deploy scripts from GitHub ($REPO)..."
rm -rf /tmp/restoos_src
git clone --depth 1 "https://github.com/${REPO}.git" /tmp/restoos_src > /dev/null 2>&1

echo ">>> [update-deploy] Applying new scripts..."
rm -rf "$ROOT/deploy.new"
cp -r /tmp/restoos_src/deploy "$ROOT/deploy.new"
rm -rf /tmp/restoos_src

chmod +x "$ROOT/deploy.new"/*.sh 2>/dev/null || true

BACKUP_DIR="$ROOT/deploy.old_$(date +%s)"
if [[ -d "$ROOT/deploy" ]]; then
  mv "$ROOT/deploy" "$BACKUP_DIR"
fi
mv "$ROOT/deploy.new" "$ROOT/deploy"
echo ">>> [update-deploy] Update complete!"
