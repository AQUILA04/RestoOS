#!/usr/bin/env bash
# Idempotent repository bootstrap for the RestoOS Cloud Agent environment.
# Java 21 and Node 22 ship in the base image; this adds Maven + Docker, builds the
# backend jar, installs frontend deps, and pre-pulls the test-infra container images.
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

echo "==> Installing system packages (Maven, Docker engine + Compose v2)"
sudo apt-get update -y
sudo apt-get install -y --no-install-recommends maven docker.io docker-compose-v2

echo "==> Configuring Docker for Docker-in-Docker (vfs storage driver)"
# Nested overlayfs mounts fail inside the agent VM, so use the vfs driver and the
# classic (non-containerd) snapshotter.
sudo mkdir -p /etc/docker
echo '{"storage-driver":"vfs","features":{"containerd-snapshotter":false}}' | sudo tee /etc/docker/daemon.json >/dev/null

echo "==> Pre-pulling test-infra images into the environment snapshot"
if ! sudo docker info >/dev/null 2>&1; then
  sudo dockerd >/tmp/dockerd-install.log 2>&1 &
  for _ in $(seq 1 30); do
    sudo docker info >/dev/null 2>&1 && break
    sleep 1
  done
fi
if sudo docker info >/dev/null 2>&1; then
  sudo docker compose -f docker-compose.test.yml pull postgres-test redis-test mailpit || true
else
  echo "WARN: Docker daemon not available during install; images will be pulled on start." >&2
fi

echo "==> Building backend jar"
( cd backend && mvn -B clean package -DskipTests )

echo "==> Installing frontend dependencies"
( cd frontend && npm ci )

echo "==> Installing Playwright Chromium (for golden-path E2E)"
( cd frontend && npx --yes playwright install --with-deps chromium )

echo "==> install.sh complete"
