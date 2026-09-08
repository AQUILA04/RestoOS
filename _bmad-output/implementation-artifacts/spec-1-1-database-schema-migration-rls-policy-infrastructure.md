---
title: 'Story 1.1: Database Schema Migration & RLS Policy Infrastructure'
type: 'feature'
created: '2026-09-08'
status: 'in-progress'
baseline_commit: 'e4e8f2c23aecc8c0d73328e41cf6d060cf4658c2'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** RestoOS needs multi-tenant database isolation at the PostgreSQL engine level to ensure absolute tenant data segregation and foundational tables for organizations, stores, users, and multi-site memberships.

**Approach:** Create Liquibase database migration scripts establishing `organizations`, `stores`, `users`, `memberships`, and `membership_stores` tables with UUID primary keys and `organization_id NOT NULL` foreign columns, enable and force Row-Level Security (RLS) policies on tenant-scoped tables using `current_setting('app.current_org_id', true)::uuid`, and create performance composite indexes.

## Boundaries & Constraints

**Always:**
- Use UUID for entity primary keys and foreign keys.
- Include `organization_id UUID NOT NULL` on `stores`, `memberships`, and `membership_stores`.
- Enable and force Row-Level Security (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY;` and `ALTER TABLE ... FORCE ROW LEVEL SECURITY;`).
- RLS policies must evaluate `organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid`.
- Database connection role used by Spring Boot backend must NOT be superuser / BYPASSRLS.

**Never:**
- Allow queries on tenant tables without `organization_id`.
- Omit foreign key constraints or indices on `organization_id` and `user_id`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Liquibase Migration Execution | Clean PostgreSQL 16 database | All 5 tables created, RLS enabled, indexes created | Liquibase rollback on error |
| Transaction without app.current_org_id | Query `stores` table when `app.current_org_id` is unset/empty | Returns 0 rows (isolated) | Standard empty result set |
| Transaction with valid app.current_org_id | `SET LOCAL app.current_org_id = 'org-uuid-1'` and query `stores` | Returns only stores belonging to `org-uuid-1` | N/A |

</frozen-after-approval>

## Code Map

- `backend/src/main/resources/db/changelog/db.changelog-master.xml` -- Master Liquibase changelog configuration
- `backend/src/main/resources/db/changelog/changes/001-initial-schema.sql` -- SQL DDL for initial tables, indexes, and RLS policies
- `backend/pom.xml` -- Spring Boot build file with Liquibase & PostgreSQL dependencies

## Tasks & Acceptance

**Execution:**
- [x] `backend/src/main/resources/db/changelog/db.changelog-master.xml` -- Create master Liquibase changelog file linking feature migrations.
- [x] `backend/src/main/resources/db/changelog/changes/001-initial-schema.sql` -- Create Liquibase migration DDL for `organizations`, `stores`, `users`, `memberships`, `membership_stores`, composite indexes `idx_memberships_user` and `idx_stores_org`, and RLS policies.

**Acceptance Criteria:**
- Given a clean PostgreSQL 16 database, when Liquibase migration script is executed, then tables `organizations`, `stores`, `users`, `memberships`, and `membership_stores` are created with UUID primary keys and `organization_id NOT NULL` columns.
- And Row-Level Security (RLS) is ENABLED and FORCED on `stores`, `memberships`, and `membership_stores` using policies checking `current_setting('app.current_org_id', true)::uuid`.
- And database indexes `idx_memberships_user` and `idx_stores_org` are created.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn liquibase:update` or Liquibase run verification -- expected: `SUCCESS`

**Manual checks (if no CLI):**
- Verify SQL schema in PostgreSQL console with `\d stores` and `\d+ stores` showing `Row Level Security: enabled`.
