---
name: RestoOS
status: final
sources:
  - "{planning_artifacts}/prds/prd-RestoOS-2026-09-08/prd.md"
updated: 2026-09-08
---

# RestoOS — Experience Spine (`EXPERIENCE.md`)

> **Information Architecture, Behavioral Patterns, States & Key User Journeys**  
> Companion artifact to `DESIGN.md`. Governs how RestoOS behaves across POS, KDS, and Admin surfaces.

---

## 1. Foundation

RestoOS is a multi-tenant, multi-surface SaaS ecosystem designed for modern restaurant chains, franchises, and independent establishments.

- **Target Surfaces**:
  1. **POS Touch Terminal** (1024px+ Touch screens / Tablets): High-speed cashier and waiter interface for ordering, table seating, modifier customization, and payment collection.
  2. **KDS Kitchen Display** (1080p+ Wall Monitors / Kitchen Tablets): Real-time ticket management for chefs and line cooks with high-visibility order timers.
  3. **Admin & Management Portal** (Desktop Web 1280px+): Management hub for multi-store configuration, central catalogue, local price overrides, floor plan editor, staff PINs, and operational audit logs.
- **Tech Stack & Architecture**: Angular SPA with Tailwind CSS & custom design tokens (cross-referenced from `DESIGN.md`). Offline-first capabilities for POS order queueing. Multi-tenancy strictly enforced via JWT and backend organization context (`SET LOCAL app.current_org_id`).

---

## 2. Information Architecture

| Surface | Access / Trigger | Primary Purpose | Key Features |
|---|---|---|---|
| **PIN Lockscreen** | App Idle / User Switch | Rapid employee authentication on shared POS | User avatars, 4-digit PIN pad, quick store switch |
| **POS Main View** | Post PIN login | Order taking & cart assembly | Category tabs, Product grid, Active cart panel, Modifiers modal |
| **Floor Plan View** | POS Top Bar "Tables" | Visual table seating & order status | Interactive room layout (Salle / Terrasse), Table occupancy colors (`{colors.status-available}`, `{colors.status-occupied}`) |
| **KDS View** | Dedicated URL / Kitchen Mode | Kitchen ticket prep & status advancement | Ticket columns (New → Preparing → Ready), Timer badges, Audio notification on new order |
| **Manager Portal** | Web login (`/admin`) | Store & catalogue management | Multi-store manager, Central Catalogue, Local Price Overrides, 86 Manager, Staff & PINs |
| **Audit & History** | Manager Portal | Sales reporting & order audit trail | Real-time sales metrics, Order breakdown, Audit history |

---

## 3. Voice and Tone

Microcopy in RestoOS is **direct, operational, and non-distracting**. Fast-paced restaurant environments demand zero ambiguity.

| Screen / Context | Do | Don't |
|---|---|---|
| **POS Cart Button** | "Envoyer en Cuisine (42.50 €)" | "Valider et soumettre votre commande" |
| **86 Stock Toggle** | "Passer en Rupture (86)" | "Désactiver temporairement ce produit du menu" |
| **KDS Ticket Action**| "Prêt · Serving" | "Marquer la commande comme étant prête" |
| **Offline Toast** | "Hors-ligne · 3 commandes en attente" | "Attention! Votre connexion Internet a été interrompue." |
| **Error Feedback** | "Prix non valide pour Store Lyon" | "Une erreur s'est produite lors de la transaction 500" |

---

## 4. Component Patterns

### 4.1 POS Product Grid & Search
- Tap a product card (`{components.pos-item-card}`) to instantly add 1 unit to cart.
- If product has required modifier groups (e.g. Cooking Level, Sauces), auto-pop the **Modifier Modal** immediately.
- Long-press or secondary tap on cart line-item opens item customization (change quantity, add special note, remove line).

### 4.2 Modifier Selection Modal
- Appears centered over POS grid (`{rounded.lg}`, shadow level 3).
- Groups displayed in sequential tabs or vertical stack: Required groups marked with red asterisk `*`.
- "Done" button stays disabled until all mandatory modifier groups have valid selections.

### 4.3 KDS Ticket Card Progression
- Cards are ordered chronologically (oldest tickets top-left).
- Single tap on ticket header or primary action button advances ticket status: `NEW` → `PREPARING` → `READY` → `SERVED`.
- Tap on individual item line strikes through item (for multi-cook station prep coordination).

### 4.4 Emergency "86" Stock Manager
- Available directly from POS header menu or Manager Portal.
- Search product -> One-tap toggle `Disponible` ↔ `Rupture (86)`.
- Instantly syncs across all POS terminals in the store via WebSocket / SSE.

---

## 5. State Patterns

| State | Surface | Visual & Behavioral Treatment |
|---|---|---|
| **Product 86 (Out of Stock)** | POS Grid | Grayscale item card with bright crimson badge "🔴 ÉPUISÉ". Item unclickable. |
| **Table Occupied** | Floor Plan Map | Table node turns Ruby Red (`{colors.status-occupied}`). Shows total bill amount & elapsed seating time. |
| **KDS Timer Urgent** | Kitchen Display | Ticket header flashes Crimson (`{colors.kds-timer-urgent}`) when ticket age > 15 minutes. |
| **Offline Mode** | POS Top Bar | Offline badge appears with pending sync count. POS continues accepting orders locally with generated UUIDs. |
| **Concurrent Table Access** | POS Map | If Waiter B opens Table 12 while Waiter A is editing, a banner shows "En cours de modification par Alice". |

---

## 6. Interaction Primitives

- **Touch-First POS**: Large hit targets (minimum `{spacing.touch-min}` = 48px, primary buttons 64px+). Swipe gestures on cart items (swipe left to delete line item).
- **Fast PIN Numpad**: On POS station lockscreen, large circular numeric buttons (`{components.pin-button}`) allow sub-2-second user switching without requiring email/password re-entry.
- **Physical Keypad / Scanner Support**: POS barcode scanner input automatically mapped to product lookup.
- **Keyboard Shortcuts (Admin Portal)**:
  - `/` — Quick search catalogue or store.
  - `N` — New Product / New Category.
  - `Esc` — Close dialogs/modals.

---

## 7. Accessibility Floor

- **WCAG 2.2 AA Compliance**: Text and status indicators meet strict contrast ratios against dark and light surfaces.
- **Kitchen High-Contrast Guarantee**: KDS text utilizes high-contrast bold fonts (`{typography.fontFamily.main}`) against dark backgrounds (`{colors.kds-bg}`) to remain legible through steam, heat, and glare.
- **Audio Feedback**: KDS emits a clear, distinct 2-tone audio chime on receiving a new kitchen ticket, configurable per kitchen station.
- **Touch Radius & Spacing**: Button elements maintain strict separation to avoid touch mis-taps during rush hours.

---

## 8. Key User Journeys

### Journey 1 — Fast Table Service Order (Waiter Alex)
1. **Context**: Friday night rush, 7:30 PM. Table 12 (4 guests) sits down. Waiter Alex approaches POS Terminal 1.
2. **PIN Entry**: Alex taps his avatar "Alex" on screen and enters 4-digit PIN `4821`. Station authenticates in under 1 second.
3. **Table Selection**: Alex taps "Plan de Salle", sees Table 12 green (`{colors.status-available}`). Taps Table 12 -> "Ouvrir la table".
4. **Order Assembly**:
   - Taps "Burgers" category -> Taps "Cheese Burger" x2.
   - Modifier modal opens automatically for cooking choice: Selects "Saignant", Sauce "BBQ", Add "Bacon (+2€)". Taps "Valider".
   - Taps "Boissons" -> Taps "Coca-Cola" x2, "Eau Minérale" x2.
5. **Kitchen Transmission**: Alex verifies cart total (48.50 €) and taps prominent orange button **"Envoyer en Cuisine"**.
6. **Climax & Result**: Table 12 turns Ruby Red on the floor plan map. Ticket instantly prints/appears on KDS Screen with chime sound. Alex's session locks back to PIN screen ready for the next staff member.

---

### Journey 2 — Kitchen Ticket Rush (Chef Marco on KDS)
1. **Context**: Kitchen line during peak dinner service. Chef Marco oversees the main hot line display.
2. **Ticket Reception**: KDS plays audio chime. Ticket **#1042 (Table 12)** appears in the `NEW` column with green timer `00:12`.
3. **Item Preparation**: Marco glances at the ticket: 2x Cheese Burger (Saignant, BBQ, +Bacon). He taps individual item lines as patties hit the grill (item turns dim strikethrough).
4. **Timer Escalation**: At 9 minutes elapsed, timer badge turns Amber (`{colors.kds-timer-warning}`). Marco accelerates plating.
5. **Completion**: At 11:35, plates are filled. Marco taps **"PRÊT"** on the ticket header.
6. **Climax & Result**: Ticket animates to the `READY` column. Waiter notification chime sounds on POS terminals.

---

### Journey 3 — Emergency "86" & Price Override (Manager Sarah)
1. **Context**: Kitchen runs out of Fresh Salmon at 8:15 PM. Manager Sarah needs to stop all POS terminals from selling it immediately.
2. **Access**: Sarah opens Manager App on her tablet, taps "Gestion Ruptures (86)".
3. **Toggle Out of Stock**: Sarah types "Saumon", taps the toggle switch to `Rupture`.
4. **Local Price Override**: While in the app, she adjusts the local price for "Tiramisu Maison" from 6.50 € to 7.00 € for Store Lyon. Taps "Enregistrer".
5. **Climax & Result**: Salmon instantly displays "🔴 ÉPUISÉ" with crimson overlay across all 4 POS terminals in the store. Subsequent attempts to select Salmon are blocked. Tiramisu updates to 7.00 € instantly on all active cashier screens without requiring app restarts.
