# RestoOS Story Traceability Matrix

> Updated 2026-09-08. Spec status: `not-started` | `in-progress` | `verified`.
> Stories become `verified` only when Chromium golden path (retries=0) and unit/IT suites are green in CI.

| Story | Spec | Evidence | Status |
|-------|------|----------|--------|
| 1.1 DB + RLS | spec-1-1 | Liquibase 001–009, `restoos_app`, CI liquibase-admin + app runtime | in-progress |
| 1.2 JWT + RLS | spec-1-2 | ProductionJwtConfig + AudienceValidator; E2e HS256; TenantContext | in-progress |
| 1.3 Org/Store APIs | spec-1-3 | Organization/Store controllers + GP Stage 1 | in-progress |
| 1.4 Memberships + invite | spec-1-4 | InvitationService HTML + `/api/v1/auth/activate` + Mailpit | in-progress |
| 1.5 Angular shell | spec-1-5 | Angular 18 shell + Nx project stubs pos/kds/admin | in-progress |
| 1.6 PIN | spec-1-6 | AuthService station JWT + rate limit; no unlock bypass | in-progress |
| 2.1 Catalogue | spec-2-1 | CatalogController + product↔modifier link | in-progress |
| 2.2 Overrides | spec-2-2 | PUT stores/{}/products/{} | in-progress |
| 2.3 Admin UI | spec-2-3 | Admin catalog page | in-progress |
| 2.4 86 stock | spec-2-4 | Store-local availability + companions | in-progress |
| 3.1 Floor APIs | spec-3-1 | `/api/v1/stores/{id}/zones|tables` + findOrCreateZone | in-progress |
| 3.2 Floor UI | spec-3-2 | POS floor plan | in-progress |
| 4.1 Orders + idempotency | renumbered | OrderService + IdempotencyService | in-progress |
| 4.2 POS UI | renumbered | pos-page + layout | in-progress |
| 4.3 Modifiers | resolved DTO | ResolvedProductDto.modifierGroups | in-progress |
| 4.4 Offline | IndexedDB | OfflineQueueService + companion | in-progress |
| 5.1 STOMP | spec-5-1 | OutboxPublisher + RedisStompConfig + WebSocket JWT | in-progress |
| 5.2 KDS UI | spec-5-2 | kds-page | in-progress |
| 5.3 Workflow | spec-5-3 | KitchenController + OrderStateMachineTest | in-progress |
| 6.1 Deliver | deliver | OrderService.deliver | in-progress |
| 6.2 Payment | mark-paid | PaymentServiceTest | in-progress |
| 6.3 Cancel/audit | cancel | manager-only cancel + ApiExceptionHandler | in-progress |
| 6.4 Dashboard | metrics | exact KPI assert in GP | in-progress |

## Automated proof (local)
- Backend: `mvn test` → 29 tests green
- Frontend: `ng build` → `dist/restoos-shell`
- E2E: CI job Compose + Keycloak + Mailpit + Playwright `retries=0`

## Deferred V2
- Digital receipt email
- EMV terminal, aggregators, Click & Collect, advanced BI
