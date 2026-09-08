---
title: 'Story 1.2: Keycloak JWT OIDC Security & RLS Transaction Interceptor'
type: 'feature'
created: '2026-09-08'
status: 'in-progress'
baseline_commit: 'ce46ba1'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Standard Spring Security JWT authentication lacks multi-tenant PostgreSQL RLS session context binding, risking cross-tenant data leakage if tenant parameters are not set at transaction initialization.

**Approach:** Integrate Spring Security OAuth2 Resource Server with JWT authentication converter for Keycloak roles (`OWNER`, `ADMIN`, `STORE_MANAGER`, `CASHIER`, `WAITER`, `KITCHEN`), and create an `RlsContextInterceptor` / filter that extracts `organization_id` and `store_id` claims from the authenticated JWT token and executes `SET LOCAL app.current_org_id = '...'` and `SET LOCAL app.current_store_id = '...'` on the current DB transaction.

## Boundaries & Constraints

**Always:**
- Strictly extract `organization_id` and `store_id` from verified JWT token claims.
- Ignore client-provided `organization_id` or `store_id` query/body overrides.
- Wrap REST payloads in `Response.builder().status(...).statusCode(...).message(...).service("RESTO-OS").data(...).build()`.

**Never:**
- Bypass JWT token signature verification.
- Execute SQL queries in a transaction without setting RLS session variables.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Valid JWT with org_id & store_id | Request header `Authorization: Bearer <valid-jwt>` | Request proceeds; `SET LOCAL app.current_org_id` and `SET LOCAL app.current_store_id` executed | N/A |
| Missing/Invalid Bearer Token | No token or invalid signature | HTTP 401 Unauthorized | Standard error envelope |
| Parameter Override Attempt | Query `?organization_id=other-org` with valid JWT | Query parameter ignored; JWT org ID enforced | N/A |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/resto/core/security/SecurityConfig.java` -- Spring Security OAuth2 resource server configuration
- `backend/src/main/java/com/resto/core/security/JwtAuthenticationConverter.java` -- Keycloak role & claim extractor
- `backend/src/main/java/com/resto/core/security/RlsContextInterceptor.java` -- HandlerInterceptor for PostgreSQL RLS SET LOCAL
- `backend/src/main/java/com/resto/core/security/RlsAspect.java` -- Transactional AOP aspect for session initialization
- `backend/src/main/java/com/resto/core/response/ApiResponse.java` -- Standardized API envelope builder

## Tasks & Acceptance

**Execution:**
- [x] `backend/src/main/java/com/resto/core/response/ApiResponse.java` -- Create standardized API response envelope builder.
- [x] `backend/src/main/java/com/resto/core/security/SecurityConfig.java` -- Configure Spring Security Resource Server & WebMvc interceptors.
- [x] `backend/src/main/java/com/resto/core/security/RlsContextInterceptor.java` -- Implement RLS session variables binding.

**Acceptance Criteria:**
- Given an incoming HTTP request containing a valid Keycloak JWT bearer token, when Spring Security processes it, then JWT claims & roles are verified and `SET LOCAL app.current_org_id` / `SET LOCAL app.current_store_id` are set on the active DB transaction.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
