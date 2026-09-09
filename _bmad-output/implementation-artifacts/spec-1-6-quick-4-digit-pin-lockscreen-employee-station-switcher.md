---
title: 'Story 1.6: Quick 4-Digit PIN Lockscreen & Employee Station Switcher'
type: 'feature'
created: '2026-09-08'
status: 'verified'
baseline_commit: 'bf6e6de'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Fast-paced restaurant stations require sub-2-second employee switching on shared POS terminals using a 4-digit PIN lockscreen with BCrypt PIN hashing.

**Approach:** Implement Spring Boot Auth REST API (`POST /api/v1/auth/pin-login`) validating 4-digit PIN against user BCrypt hash, and build Angular lockscreen component `PinLockscreenComponent` (`standalone: false`) with 72px circular `pin-button` numpad and avatar card selection.

## Boundaries & Constraints

**Always:**
- Hash 4-digit PINs using BCrypt / Argon2id.
- Retain `standalone: false` in `@Component` metadata.
- Wrap API response in `Response.builder()...`.
- Never log plain-text PINs.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Correct PIN Entry | Avatar selected + 4 valid digits | Auth session token returned < 2s | N/A |
| Incorrect PIN Entry | Invalid 4 digits | HTTP 401 Invalid PIN | Clear numpad & show error toast |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/resto/tenant/service/AuthService.java` -- PIN validation & BCrypt service
- `backend/src/main/java/com/resto/tenant/controller/AuthController.java` -- PIN auth REST controller
- `frontend/src/app/ui-components/pin-lockscreen/pin-lockscreen.component.ts` -- Angular POS PIN Lockscreen component (`standalone: false`)

## Tasks & Acceptance

**Execution:**
- [x] Create backend `AuthService` and `AuthController` for PIN verification.
- [x] Create frontend `PinLockscreenComponent` UI (`standalone: false`).

**Acceptance Criteria:**
- Given a shared POS lockscreen, entering a 4-digit PIN authenticates the employee in < 2 seconds without plain-text PIN logging.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn clean compile` -- expected: SUCCESS
