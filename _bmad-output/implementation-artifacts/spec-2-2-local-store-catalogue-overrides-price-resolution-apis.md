---
title: 'Story 2.2: Local Store Catalogue Overrides & Price Resolution APIs'
type: 'feature'
created: '2026-09-08'
status: 'done'
baseline_commit: 'fd5a189'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Store managers need ability to override product prices and availability per store location, while POS terminals require transparent price resolution (store price -> fallback to base price).

**Approach:** Implement `store_products` table DDL migration, `StoreProduct` JPA entity, repository, price resolution logic in `StoreCatalogService`, and REST endpoints (`/api/v1/catalog/stores/{storeId}/products`).

## Boundaries & Constraints

**Always:**
- Fallback to central `base_price` if no local store override exists in `store_products`.
- Wrap API payloads in `Response.builder()...`.
- Maintain `organization_id` and `store_id` RLS fields.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Store Product with Local Price | Store override price = 12.50, Base = 10.00 | Resolved Price = 12.50 | N/A |
| Store Product without Override | No `store_products` record | Resolved Price = 10.00 (Base) | N/A |

</frozen-after-approval>

## Code Map

- `backend/src/main/resources/db/changelog/changes/003-store-catalog-schema.sql` -- Store products schema
- `backend/src/main/java/com/resto/catalog/domain/StoreProduct.java` -- StoreProduct entity
- `backend/src/main/java/com/resto/catalog/repository/StoreProductRepository.java` -- StoreProduct repository
- `backend/src/main/java/com/resto/catalog/service/StoreCatalogService.java` -- Price resolution service logic
- `backend/src/main/java/com/resto/catalog/controller/StoreCatalogController.java` -- Store catalog REST API

## Tasks & Acceptance

**Execution:**
- [x] Create Liquibase migration `003-store-catalog-schema.sql`.
- [x] Create `StoreProduct` entity & repository.
- [x] Create `StoreCatalogService` with price resolution logic and REST controller.

**Acceptance Criteria:**
- Given a store location, querying resolved catalog returns local overridden prices or falls back to central base prices.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
