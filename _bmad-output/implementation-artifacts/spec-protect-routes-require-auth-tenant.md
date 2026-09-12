---
title: 'Protect operational routes — require authenticated user + tenant'
type: 'bugfix'
created: '2026-09-11'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Guests can open POS, KDS, admin pages, and tenant settings from the landing shell with no login, so the UI cannot know which user or tenant is in session in this multi-tenant app.

**Approach:** Block all operational SPA routes until a session identifies the user and their organization (tenant); hide operational shell navigation for guests and send unauthenticated visitors to login.

</frozen-after-approval>

## Implementation Notes

- Added `authGuard` (admin) requiring `access_token` + `userId` + `organizationId`, and `stationGuard` (POS/KDS) allowing that or org+store station binding for PIN unlock.
- Shell shows POS/KDS for station or tenant session; admin links only for full tenant session. Guests see brand only.
- JWT interceptor uses `clearAuth()` on 401 (keeps station org/store) and skips redirect on public app paths; logout uses full `clear()`.
- Guards store `restoos_return_url` for post-OIDC resume; auth callback honors safe same-origin paths.
- E2E seeds session via public `/` first (`seedBrowserSession` clears prior keys) so guards do not bounce before localStorage is set.
- Enabled Karma entry (`src/test.ts`); unit tests cover guards and session predicates.

## Review Triage Log

- stationGuard→Keycloak for unbound station — `false`: cold visitors must establish a tenant session; login is the intended path (PIN needs prior org/store binding).
- Shell admin links for station-only binding — `high` → patched: split `showAdminNav` / `showStationNav`.
- Missing returnUrl after login — `medium` → patched: `restoos_return_url` + auth-callback resume.
- 401 clear drops station binding — `high` → patched: `clearAuth()` keeps org/store.
- clear() misses pos_staff/legacy keys — `medium` → patched in `clear()`.
- No anonymous e2e acceptance — `medium` → deferred to deferred-work.md.
- Missing predicate unit tests — `medium` → patched with store-context.spec.ts.
- seedBrowserSession leaves stale keys — `medium` → patched: clears keys before seed.
- Route wiring untested — `low` rejected: guard unit coverage + routes are declarative; wiring test adds little.
- 401 redirect on public pages — `medium` → patched: public path allowlist includes landing/activate/callback.
