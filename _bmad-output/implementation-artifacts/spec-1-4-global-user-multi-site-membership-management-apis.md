---
title: 'Story 1.4: Global User & Multi-Site Membership Management APIs'
type: 'feature'
created: '2026-09-08'
status: 'done'
baseline_commit: 'f2d31ff'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** System managers require REST APIs to provision global users and assign multi-site memberships across stores without duplicating employee user accounts.

**Approach:** Implement JPA entities `User`, `Membership`, and `MembershipStore`, Spring Data repositories, UserService, and REST controllers (`/api/v1/users`, `/api/v1/memberships`) wrapping response payloads with `Response.builder()...`.

## Boundaries & Constraints

**Always:**
- Use UUID for entity keys and foreign keys.
- Wrap all REST payloads with `Response.builder().status(...).statusCode(...).message(...).service("RESTO-OS").data(...).build()`.
- Validate user email uniqueness and valid role assignments (`OWNER`, `ADMIN`, `STORE_MANAGER`, `CASHIER`, `WAITER`, `KITCHEN`).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Create Global User | Valid DTO (email, name) | Persisted User entity | HTTP 400 if email exists |
| Create Membership | Org ID, User ID, Role, Store IDs | Persisted Membership & MembershipStore entries | HTTP 400 if org or user missing |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/resto/tenant/domain/User.java` -- User entity
- `backend/src/main/java/com/resto/tenant/domain/Membership.java` -- Membership entity
- `backend/src/main/java/com/resto/tenant/domain/MembershipStore.java` -- MembershipStore entity
- `backend/src/main/java/com/resto/tenant/repository/UserRepository.java` -- User repository
- `backend/src/main/java/com/resto/tenant/repository/MembershipRepository.java` -- Membership repository
- `backend/src/main/java/com/resto/tenant/repository/MembershipStoreRepository.java` -- MembershipStore repository
- `backend/src/main/java/com/resto/tenant/service/UserService.java` -- User & Membership service logic
- `backend/src/main/java/com/resto/tenant/controller/UserController.java` -- UserController REST API
- `backend/src/main/java/com/resto/tenant/controller/MembershipController.java` -- MembershipController REST API

## Tasks & Acceptance

**Execution:**
- [x] Create `User`, `Membership`, and `MembershipStore` entities & repositories.
- [x] Create `UserService` business logic.
- [x] Create `UserController` and `MembershipController` REST endpoints.

**Acceptance Criteria:**
- Given an authenticated Admin user, when calling `POST /api/v1/users` or `POST /api/v1/memberships`, then global users and store memberships are persisted and returned in standardized envelopes.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
