---
title: 'Story 6.3: Operational Dashboard & Store Metrics APIs'
type: 'feature'
created: '2026-09-08'
status: 'done'
baseline_commit: '634c51d'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Store managers require real-time operational store metrics (today's revenue, order counts, unpaid orders, occupied tables count) on a manager portal dashboard.

**Approach:** Implement `DashboardMetricsDto`, `DashboardService`, and `DashboardController` (`GET /api/v1/dashboard/metrics`) aggregating store operational KPIs under tenant RLS context.

## Boundaries & Constraints

**Always:**
- Aggregate metrics strictly for the requested `store_id`.
- Wrap API payloads in `Response.builder()...`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Fetch Store Metrics | Store ID parameter | Today's revenue, order counts, occupied tables returned | N/A |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/resto/dashboard/dto/DashboardMetricsDto.java` -- Operational KPIs DTO
- `backend/src/main/java/com/resto/dashboard/service/DashboardService.java` -- Metrics aggregation service logic
- `backend/src/main/java/com/resto/dashboard/controller/DashboardController.java` -- Manager dashboard REST API

## Tasks & Acceptance

**Execution:**
- [x] Create `DashboardMetricsDto`.
- [x] Create `DashboardService` and `DashboardController` REST API.

**Acceptance Criteria:**
- Given a store manager request, real-time store KPIs are aggregated and returned wrapped in `Response.builder()`.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
