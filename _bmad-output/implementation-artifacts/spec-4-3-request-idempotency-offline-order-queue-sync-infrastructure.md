---
title: 'Story 4.3: Request Idempotency & Offline Order Queue Sync Infrastructure'
type: 'feature'
created: '2026-09-08'
status: 'verified'
baseline_commit: '5cd5b58'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Intermittent POS Wi-Fi / network drops can cause duplicate order submissions or lost order drafts.

**Approach:** Implement `IdempotencyInterceptor` enforcing `Idempotency-Key` header verification on backend mutation endpoints, build frontend `OfflineQueueService` for client-side IndexedDB order queueing, and build `OfflineStatusComponent` (`standalone: false`) toast & indicator.

## Boundaries & Constraints

**Always:**
- Enforce `Idempotency-Key` header handling on order creation.
- Retain `standalone: false` in `@Component` metadata for frontend status components.
- Wrap API payloads in `Response.builder()...`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Duplicate Request with Same Idempotency-Key | Header `Idempotency-Key: uuid-123` sent twice | Cached initial response returned | N/A |
| Network Loss on POS Terminal | Browser goes offline | Draft queued in IndexedDB, top bar shows offline badge | Auto-sync on reconnection |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/resto/core/idempotency/IdempotencyInterceptor.java` -- Backend Idempotency-Key filter/interceptor
- `frontend/src/app/core/services/offline-queue.service.ts` -- Angular IndexedDB offline queue service
- `frontend/src/app/ui-components/offline-status/offline-status.component.ts` -- Top bar offline badge component (`standalone: false`)

## Tasks & Acceptance

**Execution:**
- [x] Create backend `IdempotencyInterceptor`.
- [x] Create frontend `OfflineQueueService`.
- [x] Create `OfflineStatusComponent` (`standalone: false`).

**Acceptance Criteria:**
- Critical mutations enforce `Idempotency-Key` duplicate protection, and offline draft orders queue seamlessly in IndexedDB with top bar visual indicator.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
