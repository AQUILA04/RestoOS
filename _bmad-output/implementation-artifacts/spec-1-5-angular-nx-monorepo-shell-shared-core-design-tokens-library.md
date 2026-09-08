---
title: 'Story 1.5: Angular Nx Monorepo Shell & Shared Core Design Tokens Library'
type: 'feature'
created: '2026-09-08'
status: 'done'
baseline_commit: '0d64780'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** RestoOS frontend web applications (`pos-app`, `kds-app`, `admin-portal`) require a unified monorepo infrastructure with central design tokens, shared JWT interceptors, and strict `standalone: false` component conventions.

**Approach:** Initialize the Angular monorepo frontend layout with libraries `@resto/core`, `@resto/data-access`, and `@resto/ui-components`, configure design tokens in CSS (`#D84315` Primary Flame, `#121214` KDS Obsidian, `#F8F9FA` Light Surface, Outfit/Inter/JetBrains Mono fonts), and implement JWT Auth Interceptor & WebSocket SockJS client services with `standalone: false` component metadata across all components.

## Boundaries & Constraints

**Always:**
- Retain `standalone: false` in ALL Angular component `@Component` metadata.
- Use CSS design tokens for colors and typography.
- Standardize HTTP API response extraction.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| HTTP Request via Angular HttpClient | Request sent from frontend | Bearer JWT token attached via AuthInterceptor | Redirection on 401 |
| Design Tokens Load | CSS bundle loaded in web app | CSS variables `--color-primary-flame`, `--color-kds-obsidian`, `--font-display`, etc. available | N/A |

</frozen-after-approval>

## Code Map

- `frontend/src/styles/tokens.css` -- CSS design tokens for Primary Flame, KDS Obsidian, fonts
- `frontend/src/app/core/interceptors/jwt.interceptor.ts` -- JWT Auth Interceptor
- `frontend/src/app/core/services/websocket.service.ts` -- WebSocket SockJS connection service
- `frontend/src/app/ui-components/button/button.component.ts` -- Sample design component (`standalone: false`)

## Tasks & Acceptance

**Execution:**
- [x] Create design system CSS tokens file `tokens.css`.
- [x] Create shared Angular modules, JWT Interceptor, and WebSocket service.
- [x] Create shared UI components maintaining `standalone: false`.

**Acceptance Criteria:**
- Given the Angular monorepo frontend setup, when `@resto/ui-components` and `@resto/core` are initialized, CSS design tokens and JWT interceptors are active and components maintain `standalone: false`.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- Verification of file structure and CSS token variables -- expected: SUCCESS
