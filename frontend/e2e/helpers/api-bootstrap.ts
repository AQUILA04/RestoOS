import { randomUUID } from 'crypto';
import { APIRequestContext, expect } from '@playwright/test';

export type BootstrapTenant = {
  runId: number;
  orgId: string;
  storeId: string;
  storeIdB?: string;
  ownerToken: string;
  ownerUserId: string;
  categoryId: string;
  productId: string;
  productName: string;
  productPrice: number;
};

export type AuthHeaders = Record<string, string>;

export function authHeaders(token: string): AuthHeaders {
  return { Authorization: `Bearer ${token}` };
}

/**
 * Mint a JWT via the e2e-only /api/v1/test/token endpoint.
 */
export async function mintToken(
  request: APIRequestContext,
  backendApiUrl: string,
  body: { roles: string[]; organizationId?: string; storeId?: string; userId?: string }
): Promise<{ accessToken: string; userId: string }> {
  const res = await request.post(`${backendApiUrl}/api/v1/test/token`, { data: body });
  expect(res.status(), `mint token ${JSON.stringify(body.roles)}`).toBe(200);
  const json = await res.json();
  const accessToken = json.data.accessToken as string;
  const userId = json.data.userId as string;
  expect(accessToken).toBeTruthy();
  return { accessToken, userId };
}

/**
 * Shared companion bootstrap: OWNER token → org → store → category → product.
 * Uses unique runId = Date.now() (pass your own for multi-tenant pairs).
 */
export async function bootstrapTenant(
  request: APIRequestContext,
  backendApiUrl: string,
  options?: {
    runId?: number;
    orgPrefix?: string;
    storePrefix?: string;
    productName?: string;
    productPrice?: number;
    secondStore?: boolean;
  }
): Promise<BootstrapTenant> {
  const runId = options?.runId ?? Date.now();
  const orgPrefix = options?.orgPrefix ?? 'Companion Org';
  const storePrefix = options?.storePrefix ?? 'Companion Store';
  const productName = options?.productName ?? `Item ${runId}`;
  const productPrice = options?.productPrice ?? 12.5;

  let { accessToken: ownerToken } = await mintToken(request, backendApiUrl, {
    roles: ['OWNER'],
  });

  const orgRes = await request.post(`${backendApiUrl}/api/v1/organizations`, {
    headers: authHeaders(ownerToken),
    data: { name: `${orgPrefix} ${runId}`, code: `ORG-${runId}` },
  });
  expect(orgRes.status()).toBe(200);
  const orgId = (await orgRes.json()).data.id as string;

  ({ accessToken: ownerToken } = await mintToken(request, backendApiUrl, {
    roles: ['OWNER'],
    organizationId: orgId,
  }));

  const storeRes = await request.post(`${backendApiUrl}/api/v1/stores`, {
    headers: authHeaders(ownerToken),
    data: {
      name: `${storePrefix} A`,
      code: `ST-A-${runId}`,
      timezone: 'Europe/Paris',
      currency: 'EUR',
    },
  });
  expect(storeRes.status()).toBe(200);
  const storeId = (await storeRes.json()).data.id as string;

  ({ accessToken: ownerToken } = await mintToken(request, backendApiUrl, {
    roles: ['OWNER'],
    organizationId: orgId,
    storeId,
  }));

  // Persist a real user so audit_logs.user_id FK succeeds on mutating flows
  const userRes = await request.post(`${backendApiUrl}/api/v1/users`, {
    headers: authHeaders(ownerToken),
    data: {
      email: `owner-${runId}@companion.test`,
      firstName: 'Owner',
      lastName: `${runId}`,
    },
  });
  expect(userRes.status()).toBe(200);
  const ownerUserId = (await userRes.json()).data.id as string;

  ({ accessToken: ownerToken } = await mintToken(request, backendApiUrl, {
    roles: ['OWNER'],
    organizationId: orgId,
    storeId,
    userId: ownerUserId,
  }));

  let storeIdB: string | undefined;
  if (options?.secondStore) {
    const storeBRes = await request.post(`${backendApiUrl}/api/v1/stores`, {
      headers: authHeaders(ownerToken),
      data: {
        name: `${storePrefix} B`,
        code: `ST-B-${runId}`,
        timezone: 'Europe/Paris',
        currency: 'EUR',
      },
    });
    expect(storeBRes.status()).toBe(200);
    storeIdB = (await storeBRes.json()).data.id as string;
  }

  const catRes = await request.post(`${backendApiUrl}/api/v1/categories`, {
    headers: authHeaders(ownerToken),
    data: { name: `Cat ${runId}`, sortOrder: 1 },
  });
  expect(catRes.status()).toBe(200);
  const categoryId = (await catRes.json()).data.id as string;

  const prodRes = await request.post(`${backendApiUrl}/api/v1/products`, {
    headers: authHeaders(ownerToken),
    data: {
      categoryId,
      name: productName,
      basePrice: productPrice,
      taxRate: 10,
      active: true,
    },
  });
  expect(prodRes.status()).toBe(200);
  const productId = (await prodRes.json()).data.id as string;

  return {
    runId,
    orgId,
    storeId,
    storeIdB,
    ownerToken,
    ownerUserId,
    categoryId,
    productId,
    productName,
    productPrice,
  };
}

export async function createTakeawayOrder(
  request: APIRequestContext,
  backendApiUrl: string,
  token: string,
  body: {
    storeId: string;
    productId: string;
    quantity?: number;
    sendToKitchen?: boolean;
    notes?: string;
    idempotencyKey?: string;
  }
): Promise<{ id: string; orderNumber: number; status: string; totalAmount: number; paymentStatus: string }> {
  const idempotencyKey = body.idempotencyKey ?? randomUUID();
  const res = await request.post(`${backendApiUrl}/api/v1/orders`, {
    headers: {
      ...authHeaders(token),
      'Idempotency-Key': idempotencyKey,
    },
    data: {
      storeId: body.storeId,
      orderType: 'TAKEAWAY',
      sendToKitchen: body.sendToKitchen ?? true,
      notes: body.notes,
      items: [{ productId: body.productId, quantity: body.quantity ?? 1 }],
    },
  });
  expect(res.status(), 'create TAKEAWAY order').toBe(200);
  const order = (await res.json()).data;
  expect(order.orderNumber).toBeGreaterThan(100);
  expect(order.orderNumber).not.toBe(1001);
  return order;
}

export async function getOrder(
  request: APIRequestContext,
  backendApiUrl: string,
  token: string,
  orderId: string
) {
  return request.get(`${backendApiUrl}/api/v1/orders/${orderId}`, {
    headers: authHeaders(token),
  });
}

export async function patchKitchenStatus(
  request: APIRequestContext,
  backendApiUrl: string,
  token: string,
  orderId: string,
  status: 'PREPARING' | 'READY'
) {
  const res = await request.patch(`${backendApiUrl}/api/v1/kitchen/orders/${orderId}/status`, {
    headers: authHeaders(token),
    data: { status },
  });
  expect(res.status(), `kitchen → ${status}`).toBe(200);
  const order = (await res.json()).data;
  expect(order.status).toBe(status);
  return order;
}
