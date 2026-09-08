---
title: 'Story 3.2: Interactive Visual Floor Plan Component & Live Seating Status'
type: 'feature'
created: '2026-09-08'
status: 'in-progress'
baseline_commit: '2949086'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** POS users and waiters require a graphical floor plan view showing tables by zone (Salle, Terrasse) with live seating color statuses.

**Approach:** Build Angular `FloorPlanComponent` (`standalone: false`) with room zone tabs, 100px table nodes, 3px status borders (Available `#2E7D32`, Occupied `#C62828`, Reserved `#0277BD`), and table selection events.

## Boundaries & Constraints

**Always:**
- Retain `standalone: false` in `@Component` metadata.
- Match exact color tokens: Available Green `#2E7D32`, Occupied Ruby Red `#C62828`, Reserved Blue `#0277BD`.
- Minimum 100px table size nodes.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Render Available Table | Status = AVAILABLE | 100px table card with 3px green border (`#2E7D32`) | N/A |
| Render Occupied Table | Status = OCCUPIED | 100px table card with 3px ruby red border (`#C62828`) | N/A |

</frozen-after-approval>

## Code Map

- `frontend/src/app/ui-components/floor-plan/floor-plan.component.ts` -- Angular FloorPlan component (`standalone: false`)
- `frontend/src/app/ui-components/floor-plan/floor-plan.component.html` -- HTML layout for zone tabs and table nodes grid
- `frontend/src/app/ui-components/floor-plan/floor-plan.component.css` -- CSS styling for 100px nodes and 3px status borders

## Tasks & Acceptance

**Execution:**
- [x] Create `FloorPlanComponent` (`standalone: false`).
- [x] Create HTML template and CSS styles for 100px visual table nodes.

**Acceptance Criteria:**
- Visual floor plan displays room zones and 100px table nodes with 3px status color borders.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- Verification of component metadata and CSS status borders -- expected: SUCCESS
