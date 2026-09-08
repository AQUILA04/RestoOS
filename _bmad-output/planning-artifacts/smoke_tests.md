# RestoOS — Smoke Test Suite Specification

> **Prepared by:** Murat, Master Test Architect (`🧪`)  
> **Target:** Smoke Test Validation Suite covering all 21 User Stories across Epics 1 to 6.  
> **Strategy:** Risk-based, automated API & UI smoke tests executing in < 3 minutes in CI/CD quality gates.

---

## Suite Overview

| Epic | Smoke Tests Count | Execution Level | Target Execution Time |
|---|---|---|---|
| **Epic 1: System Foundation & Security** | 6 Tests (ST-1.1 to ST-1.6) | Integration & E2E API/UI | < 35s |
| **Epic 2: Catalogue & 86 Management** | 4 Tests (ST-2.1 to ST-2.4) | REST API & STOMP WebSocket | < 25s |
| **Epic 3: Floor Plan & Tables** | 2 Tests (ST-3.1 to ST-3.2) | API & Angular UI | < 20s |
| **Epic 4: Touch POS Order Entry** | 4 Tests (ST-4.1 to ST-4.4) | API & PWA IndexedDB | < 35s |
| **Epic 5: Real-Time KDS** | 3 Tests (ST-5.1 to ST-5.3) | STOMP WebSocket & KDS UI | < 30s |
| **Epic 6: Delivery, Payment & Audit** | 4 Tests (ST-6.1 to ST-6.4) | API & Admin Dashboard UI | < 25s |
| **TOTAL** | **23 Automated Smoke Scenarios** | **Full Stack** | **< 170 seconds** |

---

## Epic 1: System Foundation, Multi-Tenant Setup & Security Infrastructure

### 🧪 ST-1.1: Database Schema Migration & RLS Tenant Isolation
- **Story Target:** Story 1.1 (Database Migration & RLS Policy)
- **Type:** Database & SQL Integration Test
- **Given** PostgreSQL 16 database with RLS policies enabled.
- **When** `SET LOCAL app.current_org_id = 'org-tenant-A'` is executed and `SELECT * FROM stores;` is queried.
- **Then** only store records belonging to `org-tenant-A` are returned.
- **And** executing queries without setting `app.current_org_id` returns 0 rows (zero data leakage).

### 🧪 ST-1.2: Keycloak JWT OIDC Security & RLS Interceptor
- **Story Target:** Story 1.2 (Keycloak OIDC Security & RLS Interceptor)
- **Type:** REST API Security Test
- **Given** an API request with bearer JWT claim `org_id = 'org-tenant-A'`.
- **When** calling `GET /api/v1/stores` with spoofed query param `?organizationId=org-tenant-B`.
- **Then** the request returns HTTP 200 containing data strictly for `org-tenant-A`.
- **And** an unauthenticated request without a JWT bearer token returns HTTP 401 Unauthorized.

### 🧪 ST-1.3: Organization & Store CRUD API Envelope Test
- **Story Target:** Story 1.3 (Multi-Tenant Org & Store APIs)
- **Type:** REST API Contract Test
- **Given** an authenticated `OWNER` user JWT.
- **When** submitting `POST /api/v1/organizations` with name `"Burger Chain Paris"`.
- **Then** response returns HTTP 200 with JSON payload wrapped in `Response.builder()` envelope:
  `{"status": "OK", "statusCode": 200, "message": "default.message.success", "service": "RESTO-OS", "data": {"id": "uuid", "name": "Burger Chain Paris"}}`.

### 🧪 ST-1.4: Global User & Multi-Site Membership API Test
- **Story Target:** Story 1.4 (Global User & Multi-Site Membership APIs)
- **Type:** REST API Integration Test
- **Given** an existing global user account `alex@restoos.io`.
- **When** submitting `POST /api/v1/memberships` assigning Store A (`STORE_MANAGER`) and Store B (`CASHIER`).
- **Then** querying user memberships returns access permissions scoped to both stores without duplicating user accounts.

### 🧪 ST-1.5: Angular Nx Monorepo Shell & Design Tokens Verification
- **Story Target:** Story 1.5 (Nx Monorepo Shell & UI Components)
- **Type:** Angular Component Metadata & Asset Test
- **Given** the Angular Nx monorepo application builds.
- **When** inspecting component decorators across `@resto/ui-components`.
- **Then** all `@Component` metadata explicitly retains `standalone: false`.
- **And** primary brand color token resolves to `--primary-600: #D84315`.

### 🧪 ST-1.6: Sub-2s Quick PIN Lockscreen Authentication Test
- **Story Target:** Story 1.6 (Quick 4-Digit PIN Lockscreen)
- **Type:** Playwright UI Smoke Test
- **Given** shared POS terminal lockscreen view.
- **When** waiter taps avatar card "Alex" and enters 4-digit PIN `4821` on the circular `pin-button` numpad.
- **Then** authentication completes and POS active workspace opens in < 2.0 seconds.
- **And** plain text PIN never appears in network requests or browser local storage.

---

## Epic 2: Central Catalogue & Local Store Management

### 🧪 ST-2.1: Central Catalogue CRUD & Category Hierarchy Test
- **Story Target:** Story 2.1 (Central Category, Product & Modifier APIs)
- **Type:** REST API Test
- **Given** an authenticated Admin user.
- **When** submitting `POST /api/v1/categories` ("Burgers") and `POST /api/v1/products` ("Cheese Burger", `base_price = 7.50`).
- **Then** product record is created with `active = true` and `base_price = 7.50`.

### 🧪 ST-2.2: Dynamic Price Override Resolution Test
- **Story Target:** Story 2.2 (Store Catalogue Overrides & Dynamic Price Resolution)
- **Type:** Backend Integration Unit Test
- **Given** central product "Cheese Burger" with `base_price = 7.50 €`.
- **When** Store Lyon sets price override `8.00 €` (`PUT /api/v1/stores/{storeId}/products/{productId}`).
- **Then** `GET /api/v1/stores/{storeId}/products` returns `8.00 €` for Store Lyon.
- **And** Store Paris (no override) resolves price to base price `7.50 €`.

### 🧪 ST-2.3: Catalogue & Price Override Manager UI Smoke Test
- **Story Target:** Story 2.3 (Catalogue & Price Override Manager UI)
- **Type:** Playwright UI Test (`admin-portal`)
- **Given** Manager logged into `admin-portal`.
- **When** navigating to Catalogue -> Store Overrides and updating price for "Tiramisu" to `7.00 €`.
- **Then** visual notification toast "Prix local mis à jour" appears and table row updates to `7.00 €`.

### 🧪 ST-2.4: Emergency "86" Stock Toggle & WebSocket Broadcast Test
- **Story Target:** Story 2.4 (Emergency "86" Stock Manager & Broadcast)
- **Type:** E2E API + STOMP WebSocket Test
- **Given** active POS terminal listening on `/topic/store/{storeId}/pos`.
- **When** Manager toggles product "Saumon" status to `Rupture (86)` (`POST /api/v1/stores/{storeId}/products/{productId}/availability`).
- **Then** a WebSocket event `PRODUCT_AVAILABILITY_CHANGED` is received within < 500ms.
- **And** POS product grid renders crimson "🔴 ÉPUISÉ" badge and disables item click.

---

## Epic 3: Floor Plan & Table Management

### 3.1: Floor Plan Zone & Table Configuration API Test
- **Story Target:** Story 3.1 (Store Floor Plan & Table APIs)
- **Type:** REST API Test
- **Given** Store Manager authenticated.
- **When** submitting `POST /api/v1/stores/{storeId}/tables` with `zone = "Salle"`, `name = "Table 12"`, `capacity = 4`.
- **Then** table is created with `status = "AVAILABLE"`.

### 🧪 ST-3.2: Interactive Visual Floor Plan Component UI Test
- **Story Target:** Story 3.2 (Interactive Visual Floor Plan UI)
- **Type:** Playwright UI Test (`pos-app`)
- **Given** Waiter opens "Plan de Salle" view.
- **When** rendering floor plan layout.
- **Then** Table 12 displays as 100px tactile node with 3px green border (`#2E7D32`).
- **And** tapping Table 12 opens seating drawer to start Dine-in order.

---

## Epic 4: Touch POS Order Entry & Assembly

### 🧪 ST-4.1: Server Price Recalculation, Store Order Numbering & Idempotency Test
- **Story Target:** Story 4.1 (Order Domain Aggregate, Order Numbering & Idempotent API)
- **Type:** Integration & Idempotency Test
- **Given** an order payload sent with item IDs and client-manipulated total `1.00 €` and header `Idempotency-Key: 7f3e-9a12`.
- **When** submitting `POST /api/v1/orders`.
- **Then** backend recalculates real price `19.50 €` from DB, generates store order number `#1042`, and returns HTTP 200.
- **And** repeating the request with the identical `Idempotency-Key` returns HTTP 200 with the exact same order `#1042` without creating duplicate records.

### 🧪 ST-4.2: High-Tactility POS 3-Column Cart Assembly Test
- **Story Target:** Story 4.2 (High-Tactility POS 3-Column Interface)
- **Type:** Playwright UI Performance Test (`pos-app`)
- **Given** active POS session.
- **When** tapping category "Burgers" and product "Cheese Burger".
- **Then** item is added to cart column within < 100ms UI latency.
- **And** touch targets measure ≥ 48px height.

### 🧪 ST-4.3: Required & Optional Modifier Modal Validation Test
- **Story Target:** Story 4.3 (POS Required & Optional Modifier Selection Modal)
- **Type:** Angular UI Test
- **Given** product "Classic Burger" requiring modifier group "Cuisson *".
- **When** product is tapped in POS grid.
- **Then** Modifier Modal opens centered.
- **And** "Valider" button remains disabled until a choice (e.g., "Saignant") is selected.

### 🧪 ST-4.4: Offline PWA Order Drafting & Server Sync Key Exchange Test
- **Story Target:** Story 4.4 (Offline Order Drafting & Sync Queue)
- **Type:** PWA & Service Worker Offline Test
- **Given** POS terminal with network connection disabled (`offline mode`).
- **When** order is created and submitted.
- **Then** top bar displays offline status badge with pending sync count `1`.
- **And** order is queued in IndexedDB with temporary client UUID `client-uuid-999`.
- **When** network connection is restored.
- **Then** offline queue auto-syncs via `POST /api/v1/orders` and replaces local UUID with permanent server ID and order number `#1043`.

---

## Epic 5: Real-Time Kitchen Display System (KDS)

### 🧪 ST-5.1: STOMP WebSocket Order Dispatch & Latency Test (<1s)
- **Story Target:** Story 5.1 (STOMP WebSocket Dispatcher & Redis Backplane)
- **Type:** Real-Time Performance Test
- **Given** KDS client subscribed to `/topic/store/{storeId}/kitchen`.
- **When** an order is submitted at POS (`status = SENT_TO_KITCHEN`).
- **Then** `ORDER_CREATED` event payload is received by KDS client within < 1.0 second.

### 🧪 ST-5.2: KDS Obsidian Dark UI, Audio Chime & 3-Tier Timer Escalation Test
- **Story Target:** Story 5.2 (Obsidian Dark Mode KDS Interface & Timer Escalation)
- **Type:** Playwright Visual & Audio Test (`kds-app`)
- **Given** KDS screen running in kitchen mode.
- **When** new ticket `#1042` arrives.
- **Then** 2-tone audio chime plays and ticket card renders in 320px column with dark background (`#121214`).
- **And** timer badge displays Green (0-8m), shifts to Amber (`#FF9800`) at 8m, and Flashing Crimson (`#F44336`) at > 15m.

### 🧪 ST-5.3: Kitchen Ticket Progression & Line Item Strike-Through Test
- **Story Target:** Story 5.3 (Kitchen Ticket Workflow & Multi-Cook Strike-Through)
- **Type:** KDS UI Workflow Test
- **Given** ticket `#1042` displayed on KDS.
- **When** cook taps item "2x Cheese Burger".
- **Then** item line applies dim strikethrough state.
- **When** cook taps primary action button "PRÊT".
- **Then** ticket status advances to `READY` and updates PostgreSQL database.

---

## Epic 6: Order Delivery, Declarative Payment & Operations Audit

### 🧪 ST-6.1: Order Delivery Transition Test
- **Story Target:** Story 6.1 (Order Delivery API & UI)
- **Type:** REST API & UI Test
- **Given** order `#1042` in `READY` status.
- **When** staff member submits `POST /api/v1/orders/{id}/deliver`.
- **Then** order status transitions to `DELIVERED`.

### 🧪 ST-6.2: Declarative Payment Collection & State Machine Enforcement Test
- **Story Target:** Story 6.2 (Declarative Payment Collection & Audit API)
- **Type:** REST API & State Machine Test
- **Given** order `#1042` with financial status `UNPAID`.
- **When** cashier submits payment `POST /api/v1/orders/{id}/payment/mark-paid` (`method = CASH`, `amount = 19.50`).
- **Then** financial status updates to `PAID`, operational status updates to `CLOSED`, and payment record is inserted with cashier ID.
- **And** attempting to transition an order to `CLOSED` without payment is rejected with HTTP 400.

### 🧪 ST-6.3: Order Cancellation Audit & Soft Prevention Test
- **Story Target:** Story 6.3 (Order Cancellation & Immutable Audit Logging API)
- **Type:** Audit Log Test
- **Given** active order `#1043`.
- **When** Manager submits `POST /api/v1/orders/{id}/cancel` with reason `"Client parti"`.
- **Then** order status transitions to `CANCELLED` with recorded timestamp and reason.
- **And** an entry is appended to `audit_logs` table without deleting database record.

### 🧪 ST-6.4: Manager Dashboard Real-Time KPI Metric Refresh Test
- **Story Target:** Story 6.4 (Store Operational Dashboard & KPI Metrics UI)
- **Type:** Admin Portal UI Test (`admin-portal`)
- **Given** Store Manager viewing operational dashboard.
- **When** page loads or metrics refresh.
- **Then** KPI widgets for Today's Orders, Declared Revenue (€), Unpaid Orders, Active Kitchen Orders, and Occupied Tables query and render in < 500ms.

---

## Verification & Execution Instructions

To execute this Smoke Test Suite in local environment or CI/CD pipeline:

```bash
# 1. Run Backend Integration & RLS Security Smoke Tests (JUnit 5 + Testcontainers)
./gradlew test --tests "com.resto.*.SmokeTest"

# 2. Run API Idempotency & STOMP WebSocket Delivery Tests
./gradlew test --tests "com.resto.order.OrderApiSmokeTest" --tests "com.resto.kitchen.KdsStompSmokeTest"

# 3. Run Frontend Angular UI Smoke Tests (Playwright E2E)
npx playwright test --config=apps/pos-app/playwright.config.ts --grep "@smoke"
npx playwright test --config=apps/kds-app/playwright.config.ts --grep "@smoke"
```
