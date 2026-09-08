---
title: 'Story 6.2: Order Cancellation with Immutable Reason Audit Trail'
type: 'feature'
created: '2026-09-08'
status: 'in-progress'
baseline_commit: 'e47ae26'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Restaurant store managers require order cancellation capabilities with mandatory cancellation reasons and immutable audit logs without hard database record deletions.

**Approach:** Add cancellation columns (`cancelled_at`, `cancelled_by`, `cancellation_reason`) in DDL schema `007-cancellation-schema.sql`, update `Order` entity, implement `cancelOrder` method in `OrderService`, and expose REST endpoint (`POST /api/v1/orders/{id}/cancel`).

## Boundaries & Constraints

**Always:**
- Require non-empty cancellation reason.
- Record `cancelled_at`, `cancelled_by`, and audit log.
- Never hard-delete order records.
- Wrap API payloads in `Response.builder()...`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Cancel Order with Reason | Valid order + reason ("Customer left") | `status = CANCELLED`, audit log saved | N/A |
| Cancel Order without Reason | Empty reason string | Rejection with HTTP 400 | "Cancellation reason is required" |

</frozen-after-approval>

## Code Map

- `backend/src/main/resources/db/changelog/changes/007-cancellation-schema.sql` -- Cancellation columns DDL schema
- `backend/src/main/java/com/resto/order/domain/Order.java` -- Updated Order entity with cancellation fields
- `backend/src/main/java/com/resto/order/service/OrderService.java` -- Cancellation business logic
- `backend/src/main/java/com/resto/order/controller/OrderController.java` -- REST endpoint for order cancellation

## Tasks & Acceptance

**Execution:**
- [x] Create Liquibase schema `007-cancellation-schema.sql`.
- [x] Update `Order` entity with cancellation fields.
- [x] Add `cancelOrder` logic and REST endpoint in `OrderController`.

**Acceptance Criteria:**
- Canceling an order marks it `CANCELLED`, records cancellation metadata and audit log, and disallows physical DB deletion.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
