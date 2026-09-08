---
stepsCompleted:
  - "step-01-validate-prerequisites"
  - "step-02-design-epics"
  - "step-03-create-stories"
  - "step-04-final-validation"
inputDocuments:
  - "_bmad-output/planning-artifacts/prds/prd-RestoOS-2026-09-08/prd.md"
  - "_bmad-output/planning-artifacts/architecture/architecture-RestoOS-2026-09-08/ARCHITECTURE-SPINE.md"
  - "_bmad-output/planning-artifacts/ux-designs/ux-RestoOS-2026-09-08/DESIGN.md"
  - "_bmad-output/planning-artifacts/ux-designs/ux-RestoOS-2026-09-08/EXPERIENCE.md"
---

# RestoOS - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for RestoOS, decomposing the requirements from the PRD, UX Design, and Architecture requirements into implementable stories.

## Requirements Inventory

### Functional Requirements

FR1: Multi-Tenant & Organization Management — Support multiple independent organizations (chains, franchises, independent restaurants) with strict data isolation sharing the same infrastructure.
FR2: Store & Facility Hierarchy Management — Define stores/établissements under organizations, with individual store settings (timezone, currency, code, active status).
FR3: Global User & Multi-Site Membership — Support global users attached to one or multiple organizations and stores via memberships without requiring account re-creation.
FR4: Role-Based Access Control — Enforce organization-level roles (OWNER, ADMIN) and store-level roles (STORE_MANAGER, CASHIER, WAITER, KITCHEN).
FR5: Central Product & Category Catalogue — Allow defining central catalog categories, products (with base price, tax rate, image URL, active status), and modifier groups/options.
FR6: Local Store Catalogue Overrides & Price Resolution — Allow store-level price overrides and availability configuration (`store_products`), resolving prices using local price if defined, fallback to base price.
FR7: Emergency "86" Stock Management — Allow store managers to instantly mark products as out-of-stock ("86" status), hiding them from POS grids for new orders without altering existing orders.
FR8: Floor Plan & Table Management — Support store floor plan configuration by zones (Salle, Terrasse) with tables having capacity, display order, and live status (AVAILABLE, OCCUPIED, RESERVED, OUT_OF_SERVICE).
FR9: POS Touch Order Creation (Dine-in & Takeaway) — Rapid order creation for DINE_IN (with mandatory table_id or counter) and TAKEAWAY, assembling products, quantity, modifiers, line notes, and customer info.
FR10: Backend Order Validation & Price Snapshot — Strictly validate product availability/ownership, recalculate prices and taxes server-side (never trusting frontend totals), and persist immutable snapshots of product names, unit prices, tax rates, and modifier details.
FR11: Human-Readable Order Numbering — Generate sequential, human-readable order numbers per store (`order_number`) alongside technical UUID primary keys.
FR12: Dual-Dimension Order & Payment State Machine — Maintain independent Operational Status (CREATED → SENT_TO_KITCHEN → PREPARING → READY → DELIVERED → CLOSED / CANCELLED) and Financial Status (UNPAID, PAID), disallowing CLOSED + UNPAID states.
FR13: Real-Time Kitchen Display System (KDS) — KDS receives order notifications in < 1s via WebSocket STOMP upon kitchen transmission, displaying cards ordered by creation time with elapsed preparation timers (Green → Amber → Red).
FR14: KDS Ticket Workflow & Multi-Cook Line Management — Line cooks can advance tickets (SENT_TO_KITCHEN → PREPARING → READY) and tap individual item lines for station coordination.
FR15: Order Delivery & Counter Service Management — Mark orders DELIVERED / REMIS AU CLIENT for both Dine-in table service and Takeaway order number calls.
FR16: Declarative Payment Recording & Method Audit — Allow cashiers to record declarative payments (CASH, CARD, MOBILE_MONEY, OTHER), transitioning financial status from UNPAID to PAID with immutable cashier/timestamp audit logs.
FR17: Order Cancellation & Immutable Audit Trail — Support order cancellation with recorded timestamp, user ID, and reason (`cancelled_at`, `cancelled_by`, `cancellation_reason`), disallowing physical deletion.
FR18: Quick PIN Lockscreen & User Switching — POS touch terminals support rapid 4-digit PIN authentication (hashed using Argon2id/BCrypt) allowing sub-2-second employee switching on shared stations.
FR19: Operational Dashboard & Store Metrics — Manager portal displays real-time store operational KPIs (today's orders, declared revenue, unpaid orders, preparing/ready counts, occupied tables count).
FR20: Comprehensive Operation Audit Logging — Audit critical domain actions (order creation, status changes, payment marking, stock 86 toggles, price overrides, store assignments) in a centralized `audit_logs` table.

### NonFunctional Requirements

NFR1: Performance API Latency — Order creation API < 300ms; KDS WebSocket delivery latency < 1s; catalog search < 200ms; 95th percentile HTTP latency < 500ms; POS UI interaction < 100ms.
NFR2: Availability & Uptime — Service availability target ≥ 99.9% uptime for core POS and KDS operations.
NFR3: Multi-Tenant Security & Defense in Depth — 4-layer defense: Keycloak JWT OIDC → Spring Security RBAC → Tenant/Store Context Interceptor → PostgreSQL Row-Level Security (RLS). Database connection role must not possess superuser/BYPASSRLS privileges.
NFR4: Concurrent Access & Optimistic Locking — Handle concurrent order modifications using Hibernate `@Version` optimistic locking to prevent race conditions.
NFR5: Request Idempotency — Critical mutation endpoints (`POST /api/v1/orders`, `POST /api/v1/payments`) enforce request idempotency via `Idempotency-Key` HTTP header.
NFR6: Offline Order Queueing — POS supports offline order drafting using PWA Service Workers, IndexedDB, and client-generated temporary UUID keys, synchronizing seamlessly upon reconnection.
NFR7: Observability & Structured Logging — Emit structured JSON logs containing `timestamp`, `level`, `service`, `trace_id`, `organization_id`, `store_id`, and `user_id`, redacting sensitive tokens and PINs.
NFR8: Database Indexing & Query Optimization — Targeted PostgreSQL composite indexes on `(store_id, created_at)`, `(store_id, status)`, `(store_id, payment_status)`, and `(user_id)` verified with `EXPLAIN ANALYZE`.
NFR9: GDPR & Data Minimization — DINE_IN orders default to `customer_id = null` to avoid unnecessary PII storage.

### Additional Requirements

- Starter Template & Monolith Architecture: Spring Boot 3.3.x Modular Monolith (`com.resto.core`, `com.resto.tenant`, `com.resto.catalog`, `com.resto.order`, `com.resto.kitchen`, `com.resto.payment`, `com.resto.audit`) + Angular 18 Nx Monorepo (`pos-app`, `kds-app`, `admin-portal`, `@resto/core`, `@resto/data-access`, `@resto/ui-components`).
- Angular Component Metadata Convention: All Angular components must retain `standalone: false` in `@Component` decorator metadata.
- Standardized API Response Envelope: All REST endpoints wrap payloads using `Response.builder().status(...).statusCode(...).message(...).service("RESTO-OS").data(...).build()`.
- Transactional RLS Session Context: PostgreSQL RLS session parameters set per transaction via `SET LOCAL app.current_org_id = ...` and `SET LOCAL app.current_store_id = ...` prior to query execution.
- STOMP WebSocket & Redis Backplane: Real-time KDS/POS messaging using Spring STOMP WebSocket over SockJS (`/topic/store/{storeId}/kitchen`, `/topic/store/{storeId}/pos`) with Redis 7.2 Pub/Sub backplane.
- Offline Client UUID Key Exchange: Client-side generated UUID keys replaced with permanent server-assigned IDs during server synchronization.

### UX Design Requirements

UX-DR1: Dual-Theme Design System & Design Tokens — Central CSS/Tailwind design tokens covering Primary Flame palette (`#D84315`), KDS Obsidian Dark mode (`#121214`), Light Admin surfaces (`#F8F9FA`), and typography pairings (Outfit for display/headers/PIN/prices, Inter for body, JetBrains Mono for order numbers/currency).
UX-DR2: High-Tactility POS 3-Column Layout Component — Fixed 3-column POS UI layout (`[Categories 220px | Product Grid Flex | Active Cart 380px]`) with minimum touch targets of 48px/64px and instant cart item insertion.
UX-DR3: POS Modifier Selection Modal Component — Centered modal (`{rounded.lg}`, shadow level 3) for required (`*`) and optional modifier selection with disabled validation until all mandatory groups have valid selections.
UX-DR4: KDS Obsidian Kitchen Display & Timer Escalation Component — High-contrast KDS card grid (320px width), single-tap ticket status advancement, line item strike-through, 3-tier timer badge escalation (Green 0-8m → Amber 8-15m → Crimson Flashing >15m), and 2-tone audio chime on new ticket arrival.
UX-DR5: Interactive Visual Floor Plan Component — Graphical room layout viewer/editor (Salle / Terrasse) with 100px table nodes, 3px status borders, and live color coding (Available Green `#2E7D32`, Occupied Ruby Red `#C62828`, Reserved Blue `#0277BD`).
UX-DR6: Fast PIN Lockscreen Numpad Component — Shared POS lockscreen with user avatar cards, circular 72px numeric buttons (`pin-button`), sub-2-second auth flow, and quick store switching.
UX-DR7: Emergency "86" Stock Manager Interface — One-tap stock toggle UI for POS header and Manager Portal with immediate WebSocket broadcast overlay ("🔴 ÉPUISÉ" crimson badge) disabling out-of-stock items across all store terminals.
UX-DR8: Offline Mode Visual Feedback & Toast Component — Top bar status indicator and notification toast displaying offline state and count of pending unsynchronized orders.

### FR Coverage Map

FR1: Epic 1 - Multi-Tenant & Organization Management
FR2: Epic 1 - Store & Facility Hierarchy Management
FR3: Epic 1 - Global User & Multi-Site Membership
FR4: Epic 1 - Role-Based Access Control (RBAC)
FR5: Epic 2 - Central Product & Category Catalogue
FR6: Epic 2 - Local Store Catalogue Overrides & Price Resolution
FR7: Epic 2 - Emergency "86" Stock Management
FR8: Epic 3 - Floor Plan & Table Management
FR9: Epic 4 - POS Touch Order Creation (Dine-in & Takeaway)
FR10: Epic 4 - Backend Order Validation & Price Snapshot
FR11: Epic 4 - Human-Readable Store Order Numbering
FR12: Epic 4 & Epic 6 - Dual-Dimension Order & Payment State Machine
FR13: Epic 5 - Real-Time Kitchen Display System (KDS)
FR14: Epic 5 - KDS Ticket Workflow & Multi-Cook Line Management
FR15: Epic 6 - Order Delivery & Counter Service Management
FR16: Epic 6 - Declarative Payment Recording & Method Audit
FR17: Epic 6 - Order Cancellation & Immutable Audit Trail
FR18: Epic 1 - Quick PIN Lockscreen & User Switching
FR19: Epic 6 - Operational Dashboard & Store Metrics
FR20: Epic 6 - Comprehensive Operation Audit Logging

## Epic List

### Epic 1: System Foundation, Multi-Tenant Setup & Security Infrastructure
As a System Administrator or Restaurant Owner, I can configure multi-tenant organizations, stores, users, roles, and rapid PIN authentication, so that the platform enforces 4-layer PostgreSQL Row-Level Security (RLS) data isolation and provides sub-2-second employee station authentication.
**FRs covered:** FR1, FR2, FR3, FR4, FR18
**NFRs & UX-DRs covered:** NFR3, NFR7, NFR8, UX-DR1, UX-DR6

### Epic 2: Central Catalogue & Local Store Management
As an Organization Admin or Store Manager, I can manage central product categories, items, and modifiers, customize local prices and availability per store, and toggle out-of-stock ("86") items instantly, so that each restaurant location sells accurate products with local pricing and availability.
**FRs covered:** FR5, FR6, FR7
**NFRs & UX-DRs covered:** NFR1, NFR8, UX-DR7

### Epic 3: Floor Plan & Table Management
As a Store Manager or Waiter, I can design room floor plans (Salle, Terrasse) and manage visual table seating states, so that waiters can seat guests and assign orders to specific physical tables.
**FRs covered:** FR8
**NFRs & UX-DRs covered:** NFR9, UX-DR5

### Epic 4: Touch POS Order Entry & Assembly
As a Cashier or Waiter, I can assemble Dine-in and Takeaway orders on a tactile POS interface, customize item modifiers, queue orders offline during network drops, and submit validated orders with server-calculated totals and request idempotency.
**FRs covered:** FR9, FR10, FR11, FR12 (Order state machine)
**NFRs & UX-DRs covered:** NFR1, NFR4, NFR5, NFR6, UX-DR2, UX-DR3, UX-DR8

### Epic 5: Real-Time Kitchen Display System (KDS)
As a Chef or Line Cook, I can view incoming tickets on an obsidian dark-mode KDS screen in <1s, track preparation timers with visual color escalation (Green → Amber → Flashing Crimson), strike through prepared items, and advance order prep states.
**FRs covered:** FR13, FR14
**NFRs & UX-DRs covered:** NFR1, NFR2, UX-DR4

### Epic 6: Order Delivery, Declarative Payment & Operations Audit
As a Cashier or Store Manager, I can deliver ready orders to customers, collect declarative payments (Cash, Card, Mobile Money) with cashier audit trails, cancel orders with recorded reasons, and view real-time operational KPIs on a store dashboard.
**FRs covered:** FR15, FR16, FR17, FR19, FR20
**NFRs & UX-DRs covered:** NFR5, NFR7, NFR8

---

## Epic 1: System Foundation, Multi-Tenant Setup & Security Infrastructure

Goal: As a System Administrator or Restaurant Owner, I can configure multi-tenant organizations, stores, users, roles, and rapid PIN authentication, so that the platform enforces 4-layer PostgreSQL Row-Level Security (RLS) data isolation and provides sub-2-second employee station authentication.

### Story 1.1: Database Schema Migration & RLS Policy Infrastructure

As a System Administrator,
I want a database migration script establishing organizations, stores, users, and memberships tables with Row-Level Security (RLS) policies and transaction parameters,
So that all tenant data is strictly isolated at the PostgreSQL database engine level.

**Acceptance Criteria:**

**Given** a clean PostgreSQL 16 database
**When** Liquibase migration script is executed
**Then** tables `organizations`, `stores`, `users`, `memberships`, and `membership_stores` are created with UUID primary keys and `organization_id NOT NULL` columns.
**And** Row-Level Security (RLS) is ENABLED and FORCED on `stores`, `memberships`, and `membership_stores` using policies checking `current_setting('app.current_org_id', true)::uuid`.
**And** database indexes `idx_memberships_user` and `idx_stores_org` are created.

### Story 1.2: Keycloak JWT OIDC Security & RLS Transaction Interceptor

As a Backend Developer,
I want Spring Security integrated with Keycloak OIDC JWT validation and a Hibernate RLS Transaction Interceptor,
So that incoming HTTP API requests automatically extract tenant claims and set `SET LOCAL app.current_org_id` and `SET LOCAL app.current_store_id` before database queries execute.

**Acceptance Criteria:**

**Given** an incoming HTTP request containing a valid Keycloak JWT bearer token
**When** the Spring Security filter chain processes the request
**Then** JWT signature, issuer, audience, subject, and roles (`OWNER`, `ADMIN`, `STORE_MANAGER`, `CASHIER`, `WAITER`, `KITCHEN`) are verified.
**And** the custom `RlsContextInterceptor` extracts `organization_id` and `store_id` from verified claims and executes `SET LOCAL app.current_org_id = '...'` and `SET LOCAL app.current_store_id = '...'` on the active database transaction.
**And** if a client attempts to pass a custom `organization_id` in URL query parameters, the parameter is ignored and the JWT context is strictly enforced.

### Story 1.3: Multi-Tenant Organization & Store Management APIs

As an Organization Owner,
I want REST endpoints to register organizations and store locations with timezone, currency, and code settings,
So that multi-site restaurant hierarchies can be configured centralizing organizational metadata.

**Acceptance Criteria:**

**Given** an authenticated user with `OWNER` or `ADMIN` role
**When** a `POST /api/v1/organizations` or `POST /api/v1/stores` request is submitted
**Then** the organization or store record is created in the database under tenant RLS context.
**And** the REST controller wraps the response using `Response.builder().status(HttpStatus.OK).statusCode(200).message("default.message.success").service("RESTO-OS").data(result).build()`.
**And** invalid data payloads return a 400 Bad Request with standardized error details.

### Story 1.4: Global User & Multi-Site Membership Management APIs

As an Admin,
I want REST APIs to manage users and assign memberships to organizations and stores,
So that employees can be granted multi-site access across stores without re-creating user accounts.

**Acceptance Criteria:**

**Given** an authenticated Admin user
**When** calling `POST /api/v1/users` or `POST /api/v1/memberships` with user email, organization ID, store IDs, and roles (`STORE_MANAGER`, `CASHIER`, `WAITER`, `KITCHEN`)
**Then** a global `user` record is linked to `memberships` and `membership_stores`.
**And** the assigned roles determine access control boundaries across store endpoints.

### Story 1.5: Angular Nx Monorepo Shell & Shared Core Design Tokens Library

As a Frontend Developer,
I want an Angular 18 Nx Monorepo structure with `pos-app`, `kds-app`, `admin-portal` apps and `@resto/core`, `@resto/data-access`, `@resto/ui-components` libraries using `standalone: false` components and central design tokens,
So that all web apps share uniform HTTP interceptors, state models, and design system tokens.

**Acceptance Criteria:**

**Given** the Angular 18 Nx Monorepo repository
**When** `@resto/ui-components` is initialized
**Then** CSS design tokens for Primary Flame (`#D84315`), KDS Obsidian (`#121214`), Light Surface (`#F8F9FA`), and typography (Outfit, Inter, JetBrains Mono) are registered.
**And** all Angular components retain `standalone: false` in `@Component` metadata.
**And** `@resto/core` provides shared JWT HTTP interceptors and WebSocket SockJS connection services.

### Story 1.6: Quick 4-Digit PIN Lockscreen & Employee Station Switcher

As a Cashier or Waiter,
I want a shared POS lockscreen interface with employee avatar cards and a 72px circular numeric numpad,
So that terminal employees can enter a 4-digit PIN and authenticate in under 2 seconds.

**Acceptance Criteria:**

**Given** a shared POS station on the lockscreen view
**When** an employee selects their avatar card and inputs their 4-digit PIN on the `pin-button` numpad
**Then** the backend validates the PIN against the Argon2id/BCrypt hash associated with the store membership.
**And** upon successful validation, the POS session opens for that user within < 2 seconds.
**And** the PIN is never transmitted or logged in plain text.

---

## Epic 2: Central Catalogue & Local Store Management

Goal: As an Organization Admin or Store Manager, I can manage central product categories, items, and modifiers, customize local prices and availability per store, and toggle out-of-stock ("86") items instantly, so that each restaurant location sells accurate products with local pricing and availability.

### Story 2.1: Central Category, Product & Modifier Catalogue APIs

As an Organization Admin,
I want REST APIs and domain models for central categories, products, and modifier groups/options,
So that the central menu catalogue can be managed across the enterprise.

**Acceptance Criteria:**

**Given** an authenticated Admin user
**When** submitting CRUD requests to `/api/v1/categories`, `/api/v1/products`, and `/api/v1/modifier-groups`
**Then** category, product (with base_price, tax_rate, active), and modifier options are persisted in PostgreSQL.
**And** all payloads use the standardized `Response.builder()` envelope.

### Story 2.2: Store Catalogue Overrides & Dynamic Price Resolution Engine

As a Store Manager,
I want backend endpoints and price resolution logic for store-specific product overrides (`store_products`),
So that stores can customize prices or availability locally while inheriting central catalogue defaults.

**Acceptance Criteria:**

**Given** a central product with `base_price = 7.50 €`
**When** a store price override is saved (`PUT /api/v1/stores/{storeId}/products/{productId}` with `price_override = 8.00 €`)
**Then** querying store products returns `8.00 €`.
**And** if no price override exists, the system resolves the price to `base_price` (7.50 €).
**And** product vendability resolves to `(central product active AND store product available)`.

### Story 2.3: Catalogue & Price Override Manager UI (Admin Portal)

As an Organization Manager,
I want an Angular Admin Portal UI (`admin-portal`) to manage categories, products, store price overrides, and modifier groups,
So that menu management can be performed graphically with real-time feedback.

**Acceptance Criteria:**

**Given** the Admin Portal web application
**When** a manager navigates to menu management
**Then** categories, products, modifier groups, and store price override tables display cleanly with light surface styling (`#F8F9FA`).
**And** managers can update prices and save modifications with visual confirmation toasts.

### Story 2.4: Emergency "86" Stock Manager & Real-Time POS Overlay Broadcast

As a Store Manager,
I want a one-tap stock toggle UI on the POS header and Admin portal to mark products as out-of-stock ("86"),
So that out-of-stock items immediately show a crimson "🔴 ÉPUISÉ" overlay across all store POS terminals.

**Acceptance Criteria:**

**Given** a product running out of stock in the kitchen
**When** the Manager toggles the product status to `Rupture (86)` (`POST /api/v1/stores/{storeId}/products/{productId}/availability`)
**Then** a WebSocket event (`/topic/store/{storeId}/pos`) is broadcast to all active store terminals.
**And** POS product grid cards instantly update with grayscale filter 80% and a bold crimson "🔴 ÉPUISÉ" overlay.
**And** out-of-stock items cannot be added to new POS order carts.

---

## Epic 3: Floor Plan & Table Management

Goal: As a Store Manager or Waiter, I can design room floor plans (Salle, Terrasse) and manage visual table seating states, so that waiters can seat guests and assign orders to specific physical tables.

### Story 3.1: Store Floor Plan & Table Configuration APIs

As a Store Manager,
I want REST APIs to create floor plan zones and manage tables with capacity, display order, and active status,
So that restaurant room seating layouts are persisted per store.

**Acceptance Criteria:**

**Given** an authenticated Store Manager
**When** calling `POST /api/v1/stores/{storeId}/tables` with zone ("Salle", "Terrasse"), name ("Table 12"), capacity (4), and display order
**Then** table records are created in PostgreSQL associated with `store_id`.
**And** table statuses default to `AVAILABLE`.

### Story 3.2: Interactive Visual Floor Plan Component & Table Seating UI

As a Waiter or Cashier,
I want an interactive visual floor plan view in `pos-app` displaying 100px table nodes with 3px status borders and live color coding,
So that table seating, current bill totals, and seating duration can be viewed and managed visually.

**Acceptance Criteria:**

**Given** the POS application floor plan view
**When** tables are displayed on screen
**Then** table nodes render as 100px tactile elements (`{rounded.xl}`) with clear status borders: Available Green (`#2E7D32`), Occupied Ruby Red (`#C62828`), Reserved Blue (`#0277BD`).
**And** tapping an Available table opens a prompt to start a Dine-in order for that table.
**And** tapping an Occupied table displays current bill total and seating duration.

---

## Epic 4: Touch POS Order Entry & Assembly

Goal: As a Cashier or Waiter, I can assemble Dine-in and Takeaway orders on a tactile POS interface, customize item modifiers, queue orders offline during network drops, and submit validated orders with server-calculated totals and request idempotency.

### Story 4.1: Order Domain Aggregate, Store Order Numbering & Idempotent API

As a POS Terminal,
I want a backend `POST /api/v1/orders` endpoint that recalculates totals server-side, generates sequential store order numbers (`#1042`), captures item price snapshots, enforces optimistic locking (`@Version`), and requires `Idempotency-Key` headers,
So that orders are created accurately without price tampering or duplicate transactions.

**Acceptance Criteria:**

**Given** an order payload sent from `pos-app`
**When** `POST /api/v1/orders` is executed with an `Idempotency-Key` header
**Then** backend recalculates item prices and tax rates server-side from PostgreSQL (ignoring client total values).
**And** persists immutable snapshots of product names, unit prices, tax rates, and modifier option details into `order_items` and `order_item_modifiers`.
**And** generates a store-scoped sequential human-readable `order_number` (e.g. `#1042`).
**And** duplicate requests with the same `Idempotency-Key` return the cached response without creating duplicate database orders.

### Story 4.2: High-Tactility POS 3-Column Interface Component

As a Waiter or Cashier,
I want a 3-column POS UI (`[Categories 220px | Product Grid Flex | Active Cart 380px]`) in `pos-app` with 48px+ touch hit targets,
So that orders can be assembled in under 20 seconds.

**Acceptance Criteria:**

**Given** an active POS session
**When** taking an order
**Then** the UI displays 3 fixed columns: Categories sidebar (220px), Product Grid (flex), Active Cart panel (380px).
**And** all touch targets satisfy minimum height of 48px (primary action buttons 64px+).
**And** tapping a product card instantly adds 1 unit to the active cart with responsive visual feedback (< 100ms UI latency).

### Story 4.3: POS Required & Optional Modifier Selection Modal

As a Waiter,
I want a centered modal dialog over the POS grid for selecting mandatory (`*`) and optional product modifiers,
So that item choices (e.g. cooking level, sauces, extras) are captured accurately.

**Acceptance Criteria:**

**Given** a product with mandatory modifier groups (e.g., Cooking Level)
**When** the product is tapped on the POS grid
**Then** the Modifier Modal opens centered (`{rounded.lg}`, elevation level 3) displaying required groups marked with a red asterisk `*`.
**And** the "Valider" button remains disabled until all mandatory modifier choices are selected.
**And** selected modifiers display under the item line in the cart panel.

### Story 4.4: Offline Order Drafting & Sync Queue

As a Waiter on a tablet,
I want the POS app to queue draft orders in IndexedDB with temporary client UUIDs during WAN drops,
So that orders continue being taken offline and synchronize automatically upon reconnection.

**Acceptance Criteria:**

**Given** a POS terminal experiencing a Wi-Fi or WAN connectivity loss
**When** a new order is submitted
**Then** the POS top bar displays an offline status badge with pending sync count.
**And** the order is saved locally in IndexedDB using a client-generated temporary UUID key.
**And** upon connection restore, the offline queue syncs with the server via `POST /api/v1/orders` with `Idempotency-Key` header, replacing client UUIDs with permanent server-assigned IDs and store order numbers.

---

## Epic 5: Real-Time Kitchen Display System (KDS)

Goal: As a Chef or Line Cook, I can view incoming tickets on an obsidian dark-mode KDS screen in <1s, track preparation timers with visual color escalation (Green → Amber → Flashing Crimson), strike through prepared items, and advance order prep states.

### Story 5.1: Spring STOMP WebSocket Dispatcher & Redis Pub/Sub Backplane

As a KDS Terminal,
I want Spring WebSocket STOMP endpoint `/topic/store/{storeId}/kitchen` backed by Redis 7.2 Pub/Sub,
So that order creation and status events are delivered to kitchen screens in < 1 second.

**Acceptance Criteria:**

**Given** an order created at the POS terminal
**When** the order transitions to `SENT_TO_KITCHEN`
**Then** an `ORDER_CREATED` event payload is published via Spring WebSocket STOMP to `/topic/store/{storeId}/kitchen`.
**And** Redis 7.2 Pub/Sub broadcasts the message across backend cluster instances.
**And** delivery latency from POS validation to KDS reception is under 1 second.

### Story 5.2: Obsidian Dark Mode KDS Interface & 3-Tier Timer Escalation

As a Line Cook,
I want a dedicated `kds-app` interface with an obsidian dark theme (`#121214`), 320px ticket cards, 3-tier timer escalation, and 2-tone audio chimes,
So that kitchen tickets are highly visible under intense kitchen glare and heat.

**Acceptance Criteria:**

**Given** `kds-app` running on a kitchen monitor
**When** new order tickets arrive
**Then** ticket cards render in 320px columns ordered chronologically by `created_at ASC`.
**And** a 2-tone audio chime sounds upon receiving a new ticket.
**And** elapsed timers escalate badge colors: Green (0-8 min), Amber (`#FF9800`, 8-15 min), Flashing Crimson (`#F44336`, > 15 min).

### Story 5.3: Kitchen Ticket Workflow & Multi-Cook Item Strike-Through

As a Line Cook,
I want single-tap actions to advance ticket status (`SENT_TO_KITCHEN` → `PREPARING` → `READY`) and tap individual line items to strike them through,
So that preparation progress is coordinated across kitchen line stations.

**Acceptance Criteria:**

**Given** an active ticket card on the KDS display
**When** a cook taps individual item lines
**Then** the item line toggles a dim strikethrough visual state for multi-cook line coordination.
**And** tapping the primary action button advances ticket status from `SENT_TO_KITCHEN` → `PREPARING` → `READY`.
**And** status transitions update PostgreSQL and notify POS terminals in real time.

---

## Epic 6: Order Delivery, Declarative Payment & Operations Audit

Goal: As a Cashier or Store Manager, I can deliver ready orders to customers, collect declarative payments (Cash, Card, Mobile Money) with cashier audit trails, cancel orders with recorded reasons, and view real-time operational KPIs on a store dashboard.

### Story 6.1: Order Delivery & Counter Pickup Management API & UI

As a Waiter or Cashier,
I want an interface and REST API (`POST /api/v1/orders/{id}/deliver`) to mark orders DELIVERED / REMIS AU CLIENT,
So that food delivery is completed for both Dine-in table service and Takeaway number calls.

**Acceptance Criteria:**

**Given** an order in `READY` status
**When** a staff member taps "LIVRÉ" or "REMIS AU CLIENT" (`POST /api/v1/orders/{id}/deliver`)
**Then** order status transitions to `DELIVERED`.
**And** for Dine-in orders, the floor plan table status remains `OCCUPIED` until payment closure.

### Story 6.2: Declarative Payment Collection & Method Audit API

As a Cashier,
I want a payment modal and API (`POST /api/v1/orders/{id}/payment/mark-paid`) supporting declarative methods (CASH, CARD, MOBILE_MONEY, OTHER),
So that financial status transitions from `UNPAID` to `PAID` with cashier audit logging while preventing `CLOSED + UNPAID` states.

**Acceptance Criteria:**

**Given** an order with financial status `UNPAID`
**When** a cashier submits payment (`POST /api/v1/orders/{id}/payment/mark-paid` with amount, method, `recorded_by`)
**Then** a record is inserted into the `payments` table and financial status updates to `PAID`.
**And** order operational status transitions to `CLOSED`.
**And** the state machine strictly blocks transitioning an order to `CLOSED` if payment status is `UNPAID`.

### Story 6.3: Order Cancellation & Immutable Audit Logging API

As a Store Manager,
I want an API and UI (`POST /api/v1/orders/{id}/cancel`) to cancel orders with mandatory cancellation reasons,
So that cancelled orders are audited without physical database deletion.

**Acceptance Criteria:**

**Given** an active order in `CREATED`, `SENT_TO_KITCHEN`, or `PREPARING`
**When** a Manager cancels the order with a mandatory cancellation reason string
**Then** order status transitions to `CANCELLED` with recorded `cancelled_at`, `cancelled_by`, and `cancellation_reason`.
**And** an entry is recorded in `audit_logs`.
**And** physical database deletion is strictly prohibited.

### Story 6.4: Store Operational Dashboard & KPI Metrics UI

As a Store Manager,
I want an operational dashboard in `admin-portal` displaying today's total orders, declared revenue, unpaid orders, active kitchen orders, and occupied tables count,
So that store operations can be monitored in real time.

**Acceptance Criteria:**

**Given** the Admin Portal operational dashboard
**When** a manager views the store summary page
**Then** real-time KPI cards display Today's Orders count, Declared Revenue (€), Unpaid Orders count, Preparing/Ready counts, and Occupied Tables count.
**And** metrics update dynamically with under 500ms query latency.
