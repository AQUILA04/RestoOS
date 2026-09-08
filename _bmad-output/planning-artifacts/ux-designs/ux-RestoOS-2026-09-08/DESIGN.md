---
name: RestoOS
description: Multi-Tenant Restaurant SaaS Design System (POS, KDS, Admin)
status: final
created: 2026-09-08
updated: 2026-09-08

colors:
  # Primary Brand Palette (Warm Terracotta / Flame)
  primary-50: "#FFF5F2"
  primary-100: "#FFE6DF"
  primary-500: "#E65100"
  primary-600: "#D84315"
  primary-700: "#BF360C"

  # Dark Mode Surfaces (KDS & Night POS)
  kds-bg: "#121214"
  kds-surface: "#1E1E24"
  kds-border: "#2C2C35"
  kds-card-bg: "#25252D"
  kds-timer-normal: "#4CAF50"
  kds-timer-warning: "#FF9800"
  kds-timer-urgent: "#F44336"

  # Light Mode Surfaces (Admin & POS Daylight)
  surface-bg: "#F8F9FA"
  surface-card: "#FFFFFF"
  surface-sidebar: "#1A1D20"
  surface-border: "#E9ECEF"

  # Functional / Status Colors
  status-available: "#2E7D32"
  status-occupied: "#C62828"
  status-reserved: "#0277BD"
  status-out-of-stock: "#D32F2F"
  status-warning: "#ED6C02"

  # Neutral Text Palette
  text-heading: "#111827"
  text-body: "#374151"
  text-muted: "#6B7280"
  text-inverse: "#FFFFFF"

typography:
  fontFamily:
    main: "'Inter', system-ui, -apple-system, sans-serif"
    display: "'Outfit', 'Inter', system-ui, sans-serif"
    mono: "'JetBrains Mono', 'Fira Code', monospace"
  
  headings:
    h1: { fontFamily: "{typography.fontFamily.display}", fontSize: "2rem", fontWeight: "700", lineHeight: "1.2" }
    h2: { fontFamily: "{typography.fontFamily.display}", fontSize: "1.5rem", fontWeight: "600", lineHeight: "1.3" }
    h3: { fontFamily: "{typography.fontFamily.main}", fontSize: "1.25rem", fontWeight: "600", lineHeight: "1.4" }

  body:
    large: { fontFamily: "{typography.fontFamily.main}", fontSize: "1.125rem", fontWeight: "400", lineHeight: "1.5" }
    base: { fontFamily: "{typography.fontFamily.main}", fontSize: "1rem", fontWeight: "400", lineHeight: "1.5" }
    sm: { fontFamily: "{typography.fontFamily.main}", fontSize: "0.875rem", fontWeight: "400", lineHeight: "1.4" }

  pos:
    grid-title: { fontFamily: "{typography.fontFamily.display}", fontSize: "1.125rem", fontWeight: "600", lineHeight: "1.3" }
    price-tag: { fontFamily: "{typography.fontFamily.main}", fontSize: "1.25rem", fontWeight: "700", lineHeight: "1.2" }
    pin-digit: { fontFamily: "{typography.fontFamily.display}", fontSize: "2rem", fontWeight: "700", lineHeight: "1.0" }

rounded:
  sm: "4px"
  md: "8px"
  lg: "12px"
  xl: "16px"
  full: "9999px"

spacing:
  "1": "4px"
  "2": "8px"
  "3": "12px"
  "4": "16px"
  "6": "24px"
  "8": "32px"
  touch-min: "48px"
  pos-gap: "12px"

components:
  pos-item-card:
    bg: "{colors.surface-card}"
    border: "1px solid {colors.surface-border}"
    radius: "{rounded.lg}"
    padding: "{spacing.3}"
    touchHeight: "96px"

  kds-ticket-header:
    bg-normal: "{colors.kds-card-bg}"
    bg-warning: "#3E2723"
    bg-urgent: "#4A0000"
    radius: "{rounded.md}"
    padding: "{spacing.2}"

  table-node:
    radius: "{rounded.xl}"
    size: "100px"
    border-width: "3px"

  pin-button:
    bg: "{colors.surface-bg}"
    radius: "{rounded.full}"
    size: "72px"
    font: "{typography.pos.pin-digit}"
---

# RestoOS — Design System (`DESIGN.md`)

> **Visual Identity & Tokens Reference**  
> Form-factor target: Dual-surface ecosystem — High-tactility Touch POS (1024px+ tablets/all-in-ones), Thermal High-Contrast KDS displays (1080p wall mounts), and Crisp Data-Dense Admin Web (Desktop 1280px+).

---

## 1. Brand & Style

RestoOS is built for high-tempo, demanding hospitality environments where speed, clarity, and precision save seconds on every ticket.

- **Aesthetic Posture**: Industrial Warmth & Tactile Precision. It combines the warm, appetizing tones of fine dining with the bulletproof, high-contrast clarity required for smoky kitchens and bright front-of-house counters.
- **Visual Personality**: Dependable, crisp, and instant. Elements feature generous touch hit-targets, physical-feeling depth on press states, clear status coding, and zero unnecessary visual fluff.
- **Tone by Surface**:
  - **POS Surface**: High-contrast warm daylight/dark theme with bold pricing, high touch targets, and visual feedback for order builder.
  - **KDS Kitchen Surface**: Deep obsidian dark mode (`#121214`) engineered to reduce glare, with vivid glowing timer badges (Green → Amber → Red escalation).
  - **Admin & Management**: Structured, editorial light UI with high data density, elegant micro-cards, and clear hierarchy.

---

## 2. Colors

### Palette Rationale & Guidelines
- **Primary Brand (`{colors.primary-600}`)**: Warm Terracotta / Flame Orange (`#D84315`). Evokes appetite, energy, and rapid action without being overly aggressive. Used for primary CTAs, main action buttons, and active tabs.
- **KDS Dark Palette (`{colors.kds-bg}`, `{colors.kds-surface}`)**: Deep matte black/charcoal (`#121214`) designed specifically for steam and glare resistance in kitchen environments.
- **Table & Order Statuses**:
  - **Available (`{colors.status-available}`)**: Green `#2E7D32`. Indicates empty table ready for seating.
  - **Occupied (`{colors.status-occupied}`)**: Ruby Red `#C62828`. Table with active order in progress.
  - **Out of Stock / 86 (`{colors.status-out-of-stock}`)**: High-visibility Crimson `#D32F2F`. Instantly marks items unavailable on POS grids.

| Color Token | Hex | Usage | Banned Usage |
|---|---|---|---|
| `primary-600` | `#D84315` | Main CTAs, Submit Order, Selected Tabs | Table occupied state (use `status-occupied`) |
| `kds-bg` | `#121214` | KDS canvas & background | Admin reporting pages |
| `status-available` | `#2E7D32` | Available table, Cash payment confirmed | Warning toasts |
| `status-out-of-stock`| `#D32F2F` | 86 product overlay badge, urgent alert | Default button background |

---

## 3. Typography

RestoOS pairs **Outfit** (for bold, readable display heads, table numbers, and PIN digits) with **Inter** (for UI body text, modifier options, and receipt details). **JetBrains Mono** is used for order numbers (`#1042`), timestamps, and financial totals.

### Typography Ramp & Usage

| Token | Size / Weight | Application |
|---|---|---|
| `headings.h1` | 32px / 700 | Dashboard headlines, Terminal login title |
| `headings.h2` | 24px / 600 | Section headers, Floor plan zone headers |
| `headings.h3` | 20px / 600 | Category titles, Modal headers |
| `pos.grid-title`| 18px / 600 | Product card names on POS grid |
| `pos.price-tag` | 20px / 700 | Cart items total, product card prices |
| `pos.pin-digit` | 32px / 700 | Quick PIN-pad button text |
| `body.base` | 16px / 400 | Default UI text, modifier choices |
| `body.sm` | 14px / 400 | Ticket notes, sub-labels, timestamps |

---

## 4. Layout & Spacing

### Grid & Breakpoints
- **POS Screen**: Fixed 3-column layout (`[Categories: 220px | Products: Flex | Cart: 380px]`). Minimum height 768px. Touch targets maintain minimum height of `{spacing.touch-min}` (48px).
- **KDS Screen**: Horizontal flex columns with auto-wrap ticket cards (`320px` width per card), max 4 rows visible before scrolling.
- **Admin Dashboard**: Fluid responsive 12-column grid (`margin: 24px`, `gutter: 16px`).

---

## 5. Elevation & Depth

- **Level 1 (Cards & POS Items)**: `0 1px 3px rgba(0,0,0,0.08)`. Subtle separation for product cards.
- **Level 2 (Active Cart & Floating Actions)**: `0 4px 12px rgba(0,0,0,0.12)`. Applied to cart panel and floor plan selectors.
- **Level 3 (Modifer Modals & PIN Overlay)**: `0 12px 32px rgba(0,0,0,0.25)`. High-contrast shadow overlay for fast touch modals.
- **KDS Depth**: Flat high-contrast borders (`1px solid {colors.kds-border}`) instead of soft shadows to maintain sharp legibility under kitchen fluorescent lighting.

---

## 6. Shapes

- **Base Radius (`{rounded.md}` - 8px)**: Used for buttons, input fields, and category tabs.
- **Card Radius (`{rounded.lg}` - 12px)**: Product grid items, order cart container, and ticket headers.
- **Table Node Radius (`{rounded.xl}` - 16px)**: Floor plan table shapes for smooth tactile drag/drop and tap interaction.
- **Pill Radius (`{rounded.full}` - 9999px)**: PIN pad buttons, 86 status tags, and order status pills.

---

## 7. Components

### `pos-item-card`
- **Anatomy**: Product image preview (optional top half), product title (`pos.grid-title`), price tag (`pos.price-tag`), and availability badge (if out of stock).
- **States**:
  - *Default*: Surface card background, soft border.
  - *Pressed/Active*: Scaled down `0.97`, primary-100 border background.
  - *86 / Out of Stock*: Grayscale filter 80%, semi-transparent crimson overlay with bold "86 / ÉPUISÉ" badge.

### `kds-ticket-card`
- **Anatomy**: Ticket Header (Order #, Table/Takeout badge, Elapsed Timer), Item List (Quantities, Product Name, Modifiers highlighted in Amber), Footer Action ("NEXT STATE" big touch button).
- **Timer Escalation**:
  - 0-8 min: Green timer badge (`{colors.kds-timer-normal}`).
  - 8-15 min: Amber glowing border (`{colors.kds-timer-warning}`).
  - >15 min: Flashing Red header (`{colors.kds-timer-urgent}`).

### `pin-button`
- **Anatomy**: Circular button (`72px x 72px`), centered bold number, active haptic touch state.

---

## 8. Do's and Don'ts

### Do
- **Do** make all POS action buttons at least 48px high to prevent missed taps during peak rushes.
- **Do** highlight item modifiers clearly on KDS tickets with contrasting text colors (e.g. amber bold text for "Sans Oignon").
- **Do** provide instant visual feedback on POS product click (instant cart insertion with sound/vibration feedback option).

### Don't
- **Don't** use tiny text (<14px) on POS touch screens or KDS wall screens.
- **Don't** rely on hover effects for core POS or KDS functionality (must work 100% on touch screens).
- **Don't** allow scrolling inside individual KDS tickets; tickets must auto-expand or paginate cleanly.
