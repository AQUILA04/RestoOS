---
title: 'Story 4.2: Backend Order Creation, Price Snapshotting & Human-Readable Order Numbering APIs'
type: 'feature'
created: '2026-09-08'
status: 'done'
baseline_commit: '52459de'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** POS orders must strictly validate product pricing server-side, generate sequential human-readable order numbers per store, and persist immutable item snapshots to prevent retroactive price corruption.

**Approach:** Implement `005-order-schema.sql` Liquibase DDL, JPA entities `Order`, `OrderItem`, `OrderItemModifier`, repositories, `OrderService` with sequential order numbering and server-side price/tax recalculation, and REST controller (`POST /api/v1/orders`).

## Boundaries & Constraints

**Always:**
- Include `organization_id` and `store_id` RLS fields.
- Recalculate totals server-side (never trust client total).
- Persist immutable snapshots of product name, unit price, tax rate, and modifier price.
- Wrap API responses in `Response.builder()...`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Submit Valid Order | Products + Modifiers for Store A | Sequential `order_number` generated (e.g. 101), snapshots saved | N/A |
| Out of Stock / 86 Item | Order containing `is_86 = true` item | Rejection with HTTP 400 Bad Request | "Product is out of stock (86)" |

</frozen-after-approval>

## Code Map

- `backend/src/main/resources/db/changelog/changes/005-order-schema.sql` -- Orders DDL schema
- `backend/src/main/java/com/resto/order/domain/Order.java` -- Order entity
- `backend/src/main/java/com/resto/order/domain/OrderItem.java` -- OrderItem snapshot entity
- `backend/src/main/java/com/resto/order/domain/OrderItemModifier.java` -- OrderItemModifier entity
- `backend/src/main/java/com/resto/order/service/OrderService.java` -- Order creation & price snapshotting service
- `backend/src/main/java/com/resto/order/controller/OrderController.java` -- Order REST API

## Tasks & Acceptance

**Execution:**
- [x] Create Liquibase schema `005-order-schema.sql`.
- [x] Create Order domain JPA entities & repositories.
- [x] Create `OrderService` and `OrderController` REST endpoints.

**Acceptance Criteria:**
- Given a POS order submission, order is persisted with server-calculated price snapshots, sequential store `order_number`, and state `CREATED` / `UNPAID`.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
