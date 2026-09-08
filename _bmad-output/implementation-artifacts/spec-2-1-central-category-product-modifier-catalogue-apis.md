---
title: 'Story 2.1: Central Category, Product & Modifier Catalogue APIs'
type: 'feature'
created: '2026-09-08'
status: 'verified'
baseline_commit: '30d41ba'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Restaurant chains require central catalog management for menu categories, products (base price, tax rate, image), and modifier groups with options.

**Approach:** Implement Liquibase changelog for catalog tables (`categories`, `products`, `modifier_groups`, `modifier_options`), JPA entities, Spring Data repositories, CatalogService, and REST controllers (`/api/v1/categories`, `/api/v1/products`, `/api/v1/modifier-groups`).

## Boundaries & Constraints

**Always:**
- Include `organization_id UUID NOT NULL` on catalog entities for RLS.
- Use `BigDecimal` for base price and tax rate.
- Wrap REST responses in `Response.builder()...`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Create Product | Category ID, name, base_price, tax_rate | Persisted Product entity | HTTP 400 if category not found |
| Create Modifier Group | Name, min_selection, max_selection, options | Persisted ModifierGroup & Options | HTTP 400 if invalid min/max |

</frozen-after-approval>

## Code Map

- `backend/src/main/resources/db/changelog/changes/002-catalog-schema.sql` -- Catalog Liquibase schema
- `backend/src/main/java/com/resto/catalog/domain/Category.java` -- Category entity
- `backend/src/main/java/com/resto/catalog/domain/Product.java` -- Product entity
- `backend/src/main/java/com/resto/catalog/domain/ModifierGroup.java` -- ModifierGroup entity
- `backend/src/main/java/com/resto/catalog/domain/ModifierOption.java` -- ModifierOption entity
- `backend/src/main/java/com/resto/catalog/service/CatalogService.java` -- CatalogService business logic
- `backend/src/main/java/com/resto/catalog/controller/CatalogController.java` -- Catalog REST API

## Tasks & Acceptance

**Execution:**
- [x] Create Liquibase schema `002-catalog-schema.sql`.
- [x] Create JPA entities & repositories for catalog domain.
- [x] Create `CatalogService` and `CatalogController` REST APIs.

**Acceptance Criteria:**
- Given an Admin user, central categories, products, and modifier groups are persisted under tenant RLS context and returned wrapped in `Response.builder()`.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
