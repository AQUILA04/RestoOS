---
title: 'Story 2.3: Emergency "86" Stock Manager Interface & Real-time WebSocket Broadcast'
type: 'feature'
created: '2026-09-08'
status: 'done'
baseline_commit: 'c509b81'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Restaurant store managers require an instant one-tap "86" toggle UI to immediately disable sold-out products across all active store POS terminals via WebSocket broadcast.

**Approach:** Add WebSocket STOMP messaging configuration in Spring Boot (`/topic/store/{storeId}/pos`), create 86 toggle REST endpoint (`POST /api/v1/catalog/products/{productId}/86`), and build Angular `Stock86ManagerComponent` (`standalone: false`) with crimson "🔴 ÉPUISÉ" badge overlay.

## Boundaries & Constraints

**Always:**
- Broadcast STOMP event to `/topic/store/{storeId}/pos` on 86 toggle.
- Retain `standalone: false` in `@Component` metadata.
- Wrap API response in `Response.builder()...`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Toggle 86 ON | Product ID + Store ID | Product `is_86 = true`, STOMP event broadcast | N/A |
| Toggle 86 OFF | Product ID + Store ID | Product `is_86 = false`, STOMP event broadcast | N/A |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/resto/core/websocket/WebSocketConfig.java` -- Spring STOMP WebSocket configuration
- `backend/src/main/java/com/resto/catalog/controller/Stock86Controller.java` -- REST API for 86 stock toggle
- `frontend/src/app/ui-components/stock-86-manager/stock-86-manager.component.ts` -- Angular Stock 86 UI component (`standalone: false`)

## Tasks & Acceptance

**Execution:**
- [x] Configure Spring WebSocket STOMP messaging broker.
- [x] Create 86 toggle REST controller with WebSocket SimpMessagingTemplate broadcast.
- [x] Create Angular `Stock86ManagerComponent` (`standalone: false`).

**Acceptance Criteria:**
- Toggling 86 instantly updates DB and broadcasts STOMP event disabling item on POS grid with crimson badge.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
