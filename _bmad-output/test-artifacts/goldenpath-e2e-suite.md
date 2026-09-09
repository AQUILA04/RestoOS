# 🧪 RestoOS — Golden Path E2E Suite

> Updated 2026-09-08 — production-realistic acceptance (Keycloak/HMAC JWT, no auth bypass).
> Digital receipt email is **V2 / deferred**.

## Scope
1. Owner JWT → org/store provisioning
2. Manager invitation email via Mailpit (activation link)
3. Catalogue + modifiers + store price override + floor plan + staff PIN
4. Waiter PIN login → table → order with Idempotency-Key → kitchen
5. KDS ticket → strike → READY
6. Deliver → mark-paid → dashboard KPIs + audit

## Run locally
```bash
docker compose -f docker-compose.test.yml up -d --wait
cd backend && mvn -DskipTests package
RESTOOS_JWT_SECRET=e2e-secret-key-which-is-long-enough-123456 \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/restoos_test_db \
SPRING_DATASOURCE_USERNAME=restoos_admin \
SPRING_DATASOURCE_PASSWORD=restoos_password \
SPRING_MAIL_HOST=localhost SPRING_MAIL_PORT=1025 \
java -jar target/resto-os-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=e2e

cd frontend && npm ci && npm start
# other terminal
cd frontend && npm run test:e2e -- --retries=0
```

## Quality gate
- Zero retries on CI quality gate
- Exact KPI assertions (no `>= 1` pollution)
- No `#1001` fallback order numbers
- Auth required on all mutating APIs
