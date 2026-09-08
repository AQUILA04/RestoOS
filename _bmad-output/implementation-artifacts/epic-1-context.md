# Epic 1 Context: System Foundation, Multi-Tenant Setup & Security Infrastructure

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Configure multi-tenant organizations, stores, users, roles, and rapid PIN authentication, so that the platform enforces 4-layer PostgreSQL Row-Level Security (RLS) data isolation and provides sub-2-second employee station authentication.

## Stories

- Story 1.1: Database Schema Migration & RLS Policy Infrastructure
- Story 1.2: Keycloak JWT OIDC Security & RLS Transaction Interceptor
- Story 1.3: Multi-Tenant Organization & Store Management APIs
- Story 1.4: Global User & Multi-Site Membership Management APIs
- Story 1.5: Angular Nx Monorepo Shell & Shared Core Design Tokens Library
- Story 1.6: Quick 4-Digit PIN Lockscreen & Employee Station Switcher

## Requirements & Constraints

- Multi-Tenant & Organization Management: Support multiple independent organizations and stores with strict data isolation sharing the same infrastructure.
- Row-Level Security (RLS): 4-layer defense (Keycloak JWT -> Spring Security RBAC -> RlsContextInterceptor -> PostgreSQL RLS). Database connection role must not possess superuser/BYPASSRLS privileges.
- API Response Envelope: Standardized `Response.builder().status(...).statusCode(...).message(...).service("RESTO-OS").data(...).build()`.
- Angular Component Metadata: All Angular components MUST retain `standalone: false` in `@Component` metadata.
- Argon2id/BCrypt hashing for 4-digit PIN lockscreen authentication with sub-2-second station switching.

## Technical Decisions

- Architecture: Spring Boot 3.3.x Modular Monolith + Angular 18 Nx Monorepo.
- Modules: `com.resto.core`, `com.resto.tenant`, `com.resto.catalog`, `com.resto.order`, `com.resto.kitchen`, `com.resto.payment`, `com.resto.audit`.
- RLS Policy: Session variables `SET LOCAL app.current_org_id = ...` and `SET LOCAL app.current_store_id = ...`.
- User & Membership Model: Global users linked to memberships per organization and store.

## Cross-Story Dependencies

- Story 1.1 provides database tables & RLS policies required by Stories 1.2, 1.3, 1.4, 1.6.
- Story 1.2 provides Keycloak OIDC authentication and RlsContextInterceptor required by all backend API endpoints.
- Story 1.5 provides Nx Monorepo frontend structure and design tokens required by Story 1.6.
