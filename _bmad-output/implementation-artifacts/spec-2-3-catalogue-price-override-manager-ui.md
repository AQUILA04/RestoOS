# Spec 2.3 — Catalogue & Price Override Manager UI (Admin Portal)

status: 'verified'

## Story
As an Organization Manager, I want an Angular Admin Portal UI to manage categories, products, store price overrides, and modifier groups.

## Acceptance Criteria
- Admin Portal route `/admin/catalog` lists categories/products with light surface styling.
- Manager can edit store price override and sees toast "Prix local mis à jour".
- Uses canonical `PUT /api/v1/stores/{storeId}/products/{productId}`.

## Evidence required
- Playwright Stage 2 golden path price override step.
- Component/unit test for save toast.

## Deferred
None for MVP UI shell.
