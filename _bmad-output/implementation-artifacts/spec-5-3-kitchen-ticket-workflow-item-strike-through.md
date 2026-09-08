# Spec 5.3 — Kitchen Ticket Workflow & Item Strike-Through

status: 'in-progress'

## Story
As a Line Cook, I want single-tap ticket advancement and line-item strike-through coordinated across stations.

## Acceptance Criteria
- PATCH `/api/v1/kitchen/orders/{id}/status` only allows SENT_TO_KITCHEN→PREPARING→READY.
- PATCH `/api/v1/kitchen/orders/{id}/items/{itemId}/toggle` persists prepared flag.
- KDS UI strikes items with `item-done-strikethrough` and advances via `#btn-ticket-ready`.
- POS topic notified on status change.

## Evidence required
- KitchenService unit/IT transition matrix.
- Golden path Stage 4.
