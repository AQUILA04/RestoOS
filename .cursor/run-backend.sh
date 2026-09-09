#!/usr/bin/env bash
# Runs the RestoOS Spring Boot backend against the Dockerized test infra using the
# e2e profile (HS256 JWT, Postgres + Liquibase). Started as a Cloud Agent terminal.
set -euo pipefail
cd "$(dirname "$0")/.."

export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/restoos_test_db"
export SPRING_DATASOURCE_USERNAME="restoos_app"
export SPRING_DATASOURCE_PASSWORD="restoos_app_pass"
export SPRING_LIQUIBASE_USER="restoos_admin"
export SPRING_LIQUIBASE_PASSWORD="restoos_password"
export SPRING_MAIL_HOST="localhost"
export SPRING_MAIL_PORT="1025"
export SPRING_REDIS_HOST="localhost"
export RESTOOS_JWT_SECRET="e2e-secret-key-which-is-long-enough-123456"
export RESTOOS_ISSUER_URI=""
export RESTOOS_FRONTEND_BASE_URL="http://localhost:4200"

JAR="backend/target/resto-os-backend-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR" ]; then
  echo "Backend jar not found, building..."
  ( cd backend && mvn -B clean package -DskipTests )
fi

echo "Waiting for Postgres on localhost:5432 ..."
for _ in $(seq 1 60); do
  (exec 3<>/dev/tcp/127.0.0.1/5432) 2>/dev/null && { exec 3<&-; break; }
  sleep 2
done

exec java -jar "$JAR" --spring.profiles.active=e2e
