---
title: 'Story 3.1: Floor Plan Zone & Table Management APIs'
type: 'feature'
created: '2026-09-08'
status: 'in-progress'
baseline_commit: 'ef8f3bf'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Restaurant locations require floor plan layout configuration by zones (Salle, Terrasse) with physical table seating nodes, capacity, and live status.

**Approach:** Implement `004-floor-plan-schema.sql` migration, JPA entities `Zone` and `RestaurantTable`, repositories, `FloorPlanService`, and REST controller (`/api/v1/floor-plan/zones`, `/api/v1/floor-plan/tables`).

## Boundaries & Constraints

**Always:**
- Include `organization_id` and `store_id` for tenant RLS.
- Enforce valid table statuses: `AVAILABLE`, `OCCUPIED`, `RESERVED`, `OUT_OF_SERVICE`.
- Wrap API payloads in `Response.builder()...`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Create Zone | Store ID, name ("Salle") | Persisted Zone entity | HTTP 400 if store invalid |
| Create Table | Zone ID, table number, capacity, (x,y) | Persisted RestaurantTable entity | HTTP 400 if zone invalid |

</frozen-after-approval>

## Code Map

- `backend/src/main/resources/db/changelog/changes/004-floor-plan-schema.sql` -- Floor plan DDL schema
- `backend/src/main/java/com/resto/floorplan/domain/Zone.java` -- Zone entity
- `backend/src/main/java/com/resto/floorplan/domain/RestaurantTable.java` -- Table entity
- `backend/src/main/java/com/resto/floorplan/service/FloorPlanService.java` -- FloorPlanService business logic
- `backend/src/main/java/com/resto/floorplan/controller/FloorPlanController.java` -- FloorPlan REST API

## Tasks & Acceptance

**Execution:**
- [x] Create Liquibase schema `004-floor-plan-schema.sql`.
- [x] Create `Zone` and `RestaurantTable` entities & repositories.
- [x] Create `FloorPlanService` and `FloorPlanController`.

**Acceptance Criteria:**
- Given a store location, zones and tables are persisted under tenant RLS context and returned wrapped in `Response.builder()`.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
