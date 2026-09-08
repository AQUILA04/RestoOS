---
title: 'Story 1.3: Multi-Tenant Organization & Store Management APIs'
type: 'feature'
created: '2026-09-08'
status: 'done'
baseline_commit: '537c25e'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** RestoOS organization owners and administrators require REST APIs to provision and configure multi-tenant organization metadata and store locations with timezone, currency, and store code settings.

**Approach:** Implement JPA entities `Organization` and `Store`, Spring Data repositories, DTOs, domain service, and REST controllers (`/api/v1/organizations`, `/api/v1/stores`) enforcing `OWNER` / `ADMIN` roles and wrapping output payloads with `Response.builder()...`.

## Boundaries & Constraints

**Always:**
- Secure endpoints with `@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")`.
- Wrap all responses with `Response.builder().status(...).statusCode(...).message(...).service("RESTO-OS").data(...).build()`.
- Enforce unique codes on organization and (org, store) code.

**Never:**
- Allow unauthenticated requests or store creation without valid organization ID.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Create Organization | Valid DTO (name, code) | Created Organization entity wrapped in Response | HTTP 400 if code duplicate |
| Create Store | Valid DTO under active org | Created Store entity wrapped in Response | HTTP 400 if org not found |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/resto/tenant/domain/Organization.java` -- Organization entity
- `backend/src/main/java/com/resto/tenant/domain/Store.java` -- Store entity
- `backend/src/main/java/com/resto/tenant/repository/OrganizationRepository.java` -- Organization repository
- `backend/src/main/java/com/resto/tenant/repository/StoreRepository.java` -- Store repository
- `backend/src/main/java/com/resto/tenant/service/TenantService.java` -- Tenant & Store service logic
- `backend/src/main/java/com/resto/tenant/controller/OrganizationController.java` -- REST API controller for orgs
- `backend/src/main/java/com/resto/tenant/controller/StoreController.java` -- REST API controller for stores

## Tasks & Acceptance

**Execution:**
- [x] Create `Organization` and `Store` JPA entities and repositories.
- [x] Create `TenantService` handling org & store CRUD business logic.
- [x] Create REST controllers with `@PreAuthorize` role security and `Response.builder()` envelopes.

**Acceptance Criteria:**
- Given an authenticated user with `OWNER` or `ADMIN` role, when calling `POST /api/v1/organizations` or `POST /api/v1/stores`, then the records are persisted and returned wrapped in `Response.builder()`.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
