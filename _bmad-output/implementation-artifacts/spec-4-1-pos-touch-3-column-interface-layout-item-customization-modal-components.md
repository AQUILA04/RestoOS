---
title: 'Story 4.1: POS Touch 3-Column Interface Layout & Item Customization Modal Components'
type: 'feature'
created: '2026-09-08'
status: 'verified'
baseline_commit: '3bd680f'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Fast tactile POS order entry requires a high-efficiency 3-column layout `[Categories 220px | Product Grid Flex | Cart 380px]` and modifier selection modal for item customization.

**Approach:** Build Angular components `PosLayoutComponent` and `ModifierModalComponent` (`standalone: false`) with 48px/64px touch targets and modifier validation.

## Boundaries & Constraints

**Always:**
- Retain `standalone: false` in `@Component` metadata.
- 3-column width constraint: Categories 220px, Active Cart 380px, Product Grid Flex.
- Validate required modifier groups before enabling modal confirm button.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Select Product with Required Modifiers | Tap Product card | `ModifierModalComponent` opens | Confirm button disabled until mandatory modifiers selected |
| Add Item to Cart | Valid modifier selection | Cart updated with resolved price and line notes | N/A |

</frozen-after-approval>

## Code Map

- `frontend/src/app/ui-components/pos-layout/pos-layout.component.ts` -- POS 3-column layout component (`standalone: false`)
- `frontend/src/app/ui-components/modifier-modal/modifier-modal.component.ts` -- Item modifier selection modal (`standalone: false`)

## Tasks & Acceptance

**Execution:**
- [x] Create `PosLayoutComponent` with 3-column flex layout (`standalone: false`).
- [x] Create `ModifierModalComponent` with required selection validation (`standalone: false`).

**Acceptance Criteria:**
- 3-column POS layout renders with fixed 220px categories and 380px cart columns, and modifier modal enforces required selections.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- Verification of 3-column layout widths and component metadata -- expected: SUCCESS
