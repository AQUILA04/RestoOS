---
title: 'Story 5.1: Real-Time KDS STOMP Dispatcher & Order Ticket Workflow APIs'
type: 'feature'
created: '2026-09-08'
status: 'verified'
baseline_commit: '3140b8a'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Kitchen display systems require real-time STOMP dispatching under 1 second when orders are sent to kitchen or advanced through preparation states (`SENT_TO_KITCHEN` -> `PREPARING` -> `READY`).

**Approach:** Implement `KitchenService` and `KitchenController` REST API (`PATCH /api/v1/kitchen/orders/{id}/status`) broadcasting STOMP payloads to `/topic/store/{storeId}/kitchen`.

## Boundaries & Constraints

**Always:**
- Broadcast STOMP messages to `/topic/store/{storeId}/kitchen`.
- Enforce valid state machine transitions.
- Wrap API payloads in `Response.builder()...`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Advance Order to PREPARING | Order ID + Status `PREPARING` | Status updated + STOMP broadcast < 1s | N/A |
| Advance Order to READY | Order ID + Status `READY` | Status updated + STOMP broadcast < 1s | N/A |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/resto/kitchen/service/KitchenService.java` -- Kitchen domain service
- `backend/src/main/java/com/resto/kitchen/controller/KitchenController.java` -- KDS REST API

## Tasks & Acceptance

**Execution:**
- [x] Create `KitchenService` for kitchen ticket state transitions.
- [x] Create `KitchenController` with STOMP WebSocket dispatching.

**Acceptance Criteria:**
- Kitchen status updates trigger database state change and real-time STOMP message broadcast.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
