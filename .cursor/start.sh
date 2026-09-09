#!/usr/bin/env bash
# Per-boot startup: bring up the Docker daemon and the test-infra containers
# (Postgres, Redis, Mailpit). The backend and frontend run as terminals.
set -euo pipefail

echo "==> Ensuring Docker daemon configuration (vfs storage driver)"
sudo mkdir -p /etc/docker
echo '{"storage-driver":"vfs","features":{"containerd-snapshotter":false}}' | sudo tee /etc/docker/daemon.json >/dev/null

if ! sudo docker info >/dev/null 2>&1; then
  echo "==> Starting Docker daemon"
  sudo dockerd >/tmp/dockerd.log 2>&1 &
  for _ in $(seq 1 30); do
    sudo docker info >/dev/null 2>&1 && break
    sleep 1
  done
fi
sudo docker info >/dev/null 2>&1 || { echo "ERROR: Docker daemon failed to start"; tail -n 20 /tmp/dockerd.log || true; exit 1; }

echo "==> Starting test infra (Postgres, Redis, Mailpit)"
# Mailpit's bundled healthcheck is unreliable (no wget in the image), so we start
# without --wait and gate readiness on Postgres, which the backend depends on.
sudo docker compose -f docker-compose.test.yml up -d postgres-test redis-test mailpit

echo "==> Waiting for Postgres to accept connections"
for _ in $(seq 1 60); do
  if sudo docker exec restoos-postgres-test pg_isready -U restoos_admin -d restoos_test_db >/dev/null 2>&1; then
    echo "==> Postgres ready"
    exit 0
  fi
  sleep 2
done

echo "ERROR: Postgres did not become ready in time" >&2
sudo docker compose -f docker-compose.test.yml ps || true
exit 1
