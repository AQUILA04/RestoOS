# RestoOS API Contract (Canonical)

> Source of truth for routes, DTOs, roles, and state machines.
> Status values: `not-started` | `in-progress` | `verified`
> Last updated: 2026-09-08

## Auth model

- Production & golden-path acceptance: Keycloak OIDC JWT.
- Claims required: `sub`, `organization_id`, `store_id` (optional for OWNER/ADMIN), `roles`.
- Client-supplied `organizationId` / `storeId` / `userId` / `X-Tenant-ID` are ignored when they conflict with JWT claims.
- Station PIN login: `POST /api/v1/auth/pin-login` returns a short-lived station JWT scoped to store membership.

## Roles

| Role | Scope |
|------|-------|
| OWNER | Organization |
| ADMIN | Organization |
| STORE_MANAGER | Store |
| CASHIER | Store |
| WAITER | Store |
| KITCHEN | Store |

## Envelope

```json
{
  "status": "OK",
  "statusCode": 200,
  "message": "default.message.success",
  "service": "RESTO-OS",
  "data": {}
}
```

Errors use the same envelope with `statusCode` 4xx/5xx and `data` containing `{ "code", "details" }`.

## Order operational status

`CREATED → SENT_TO_KITCHEN → PREPARING → READY → DELIVERED → CLOSED`  
Alt: any of `CREATED|SENT_TO_KITCHEN|PREPARING` → `CANCELLED`

## Payment status

`UNPAID | PAID`  
Invariant: `CLOSED` requires `PAID`. `CLOSED + UNPAID` is forbidden.

## Table status

`AVAILABLE | OCCUPIED | RESERVED | OUT_OF_SERVICE`  
Dine-in order creation: `AVAILABLE → OCCUPIED`. Close/cancel releases to `AVAILABLE` when no other open dine-in order remains.

## Canonical routes

### Tenant
| Method | Path | Roles |
|--------|------|-------|
| POST | `/api/v1/organizations` | authenticated bootstrap / OWNER |
| GET | `/api/v1/organizations` | OWNER, ADMIN |
| PATCH | `/api/v1/organizations/{id}` | OWNER, ADMIN, STORE_MANAGER — name, logoUrl, mobileMoneyLabel |
| POST | `/api/v1/stores` | OWNER, ADMIN |
| GET | `/api/v1/stores` | OWNER, ADMIN, STORE_MANAGER |
| PATCH | `/api/v1/stores/{id}` | OWNER, ADMIN — name, currency, timezone, active |
| POST | `/api/v1/users` | OWNER, ADMIN |
| GET | `/api/v1/memberships` | OWNER, ADMIN — list org members |
| POST | `/api/v1/memberships` | OWNER, ADMIN |
| PATCH | `/api/v1/memberships/{id}` | OWNER, ADMIN — role, storeIds, active |
| POST | `/api/v1/memberships/invite` | OWNER, ADMIN |
| POST | `/api/v1/auth/pin-login` | public (rate-limited) |
| POST | `/api/v1/auth/set-pin` | OWNER, ADMIN, STORE_MANAGER |

### Catalog
| Method | Path | Roles |
|--------|------|-------|
| POST/GET/PUT/DELETE | `/api/v1/categories` | OWNER, ADMIN (+ GET for store staff) |
| POST/GET/PUT/DELETE | `/api/v1/products` | OWNER, ADMIN (+ GET for store staff); products include `avgPrepMinutes` |
| POST/GET | `/api/v1/modifier-groups` | OWNER, ADMIN |
| POST | `/api/v1/modifier-options` | OWNER, ADMIN |
| PUT | `/api/v1/stores/{storeId}/products/{productId}` | OWNER, ADMIN, STORE_MANAGER |
| POST | `/api/v1/stores/{storeId}/products/{productId}/availability` | OWNER, ADMIN, STORE_MANAGER |
| GET | `/api/v1/stores/{storeId}/products` | store staff |

### Floor plan
| Method | Path | Roles |
|--------|------|-------|
| POST/GET | `/api/v1/stores/{storeId}/zones` | STORE_MANAGER+ write; WAITER/CASHIER read |
| PATCH/DELETE | `/api/v1/stores/{storeId}/zones/{zoneId}` | OWNER, ADMIN, STORE_MANAGER |
| POST/GET | `/api/v1/stores/{storeId}/tables` | STORE_MANAGER+ write; WAITER/CASHIER read |
| PATCH/DELETE | `/api/v1/stores/{storeId}/tables/{tableId}` | OWNER, ADMIN, STORE_MANAGER |
| PATCH | `/api/v1/stores/{storeId}/tables/{tableId}/status` | WAITER, CASHIER, STORE_MANAGER+ |

### Orders / Kitchen / Payment
| Method | Path | Roles |
|--------|------|-------|
| POST | `/api/v1/orders` | CASHIER, WAITER, STORE_MANAGER+ — **requires `Idempotency-Key`** |
| GET | `/api/v1/orders` | store staff |
| GET | `/api/v1/orders/{id}` | store staff |
| POST | `/api/v1/orders/{id}/send-to-kitchen` | CASHIER, WAITER, STORE_MANAGER+ |
| POST | `/api/v1/orders/{id}/deliver` | CASHIER, WAITER, STORE_MANAGER+ |
| POST | `/api/v1/orders/{id}/payment/mark-paid` | CASHIER, STORE_MANAGER+ — **requires `Idempotency-Key`** |
| POST | `/api/v1/orders/{id}/cancel` | OWNER, ADMIN, STORE_MANAGER |
| PATCH | `/api/v1/kitchen/orders/{id}/status` | KITCHEN, STORE_MANAGER+ |
| PATCH | `/api/v1/kitchen/orders/{id}/items/{itemId}/toggle` | KITCHEN, STORE_MANAGER+ |
| GET | `/api/v1/kitchen/orders` | KITCHEN, STORE_MANAGER+ |
| GET | `/api/v1/dashboard/metrics?storeId=` | OWNER, ADMIN, STORE_MANAGER |
| GET | `/api/v1/audit-logs` | OWNER, ADMIN, STORE_MANAGER |

### WebSocket
- Endpoint: `/ws` (SockJS)
- Topics: `/topic/store/{storeId}/kitchen`, `/topic/store/{storeId}/pos`
- CONNECT requires JWT; SUBSCRIBE requires store membership.

## Deferred (V2)
- Digital payment receipt email
- EMV terminal integration
- Delivery aggregators / Click & Collect
- Advanced BI
