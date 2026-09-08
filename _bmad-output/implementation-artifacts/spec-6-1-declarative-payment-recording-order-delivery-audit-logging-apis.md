---
title: 'Story 6.1: Declarative Payment Recording, Order Delivery & Audit Logging APIs'
type: 'feature'
created: '2026-09-08'
status: 'verified'
baseline_commit: '5bea999'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Cashiers need declarative payment collection (Cash, Card, Mobile Money) with immutable cashier audit trails and dual-dimension state machine enforcement (disallowing CLOSED + UNPAID orders).

**Approach:** Create `006-payment-audit-schema.sql` migration, JPA entities `Payment` and `AuditLog`, repositories, `PaymentService` handling settlement and audit entry, and REST controller (`POST /api/v1/payments`).

## Boundaries & Constraints

**Always:**
- Include `organization_id` and `store_id` RLS fields on payments and audit logs.
- Disallow `CLOSED` state if `payment_status` is `UNPAID`.
- Insert audit log entry on payment recording.
- Wrap API payloads in `Response.builder()...`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Record Declarative Payment | Order ID + Payment Method + Amount | Payment saved, `payment_status = PAID`, Audit log written | N/A |
| Close Unpaid Order | Transition to CLOSED while UNPAID | Rejection with HTTP 400 | "Cannot close UNPAID order" |

</frozen-after-approval>

## Code Map

- `backend/src/main/resources/db/changelog/changes/006-payment-audit-schema.sql` -- Payments & audit schema
- `backend/src/main/java/com/resto/payment/domain/Payment.java` -- Payment entity
- `backend/src/main/java/com/resto/audit/domain/AuditLog.java` -- AuditLog entity
- `backend/src/main/java/com/resto/payment/service/PaymentService.java` -- Payment recording & state machine enforcement logic
- `backend/src/main/java/com/resto/payment/controller/PaymentController.java` -- Payment REST API

## Tasks & Acceptance

**Execution:**
- [x] Create Liquibase schema `006-payment-audit-schema.sql`.
- [x] Create `Payment` and `AuditLog` entities & repositories.
- [x] Create `PaymentService` and `PaymentController`.

**Acceptance Criteria:**
- Declarative payments settle orders to `PAID`, record cashier audit trails, and enforce state machine constraints.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
