---
title: 'Story 5.2: Obsidian Dark Mode KDS Ticket Grid & Preparation Timer Escalation Component'
type: 'feature'
created: '2026-09-08'
status: 'done'
baseline_commit: '239663a'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Line cooks require a tactile obsidian dark-mode KDS ticket display with 3-tier timer escalation (Green 0-8m, Amber 8-15m, Crimson >15m) and single-tap status advancement.

**Approach:** Build Angular `KdsGridComponent` (`standalone: false`) with obsidian surface (`#121214`), 320px card width, elapsed time counter, and 3-tier timer badge escalation.

## Boundaries & Constraints

**Always:**
- Retain `standalone: false` in `@Component` metadata.
- Obsidian dark surface `#121214` and 320px card width.
- Timer escalation tiers: Green (0-8m), Amber (8-15m), Crimson Flashing (>15m).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Ticket Under 8 Minutes | Elapsed = 5m | Green timer badge | N/A |
| Ticket Over 15 Minutes | Elapsed = 18m | Crimson flashing timer badge | N/A |

</frozen-after-approval>

## Code Map

- `frontend/src/app/ui-components/kds-grid/kds-grid.component.ts` -- KDS Grid component (`standalone: false`)
- `frontend/src/app/ui-components/kds-grid/kds-grid.component.html` -- HTML layout for obsidian KDS cards
- `frontend/src/app/ui-components/kds-grid/kds-grid.component.css` -- CSS for obsidian dark theme and flashing timers

## Tasks & Acceptance

**Execution:**
- [x] Create `KdsGridComponent` (`standalone: false`).
- [x] Create obsidian dark mode styles and 3-tier timer escalation rules.

**Acceptance Criteria:**
- KDS ticket grid renders on obsidian dark background with 320px cards and 3-tier preparation timer escalation.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- Verification of obsidian dark styles and timer escalation rules -- expected: SUCCESS
