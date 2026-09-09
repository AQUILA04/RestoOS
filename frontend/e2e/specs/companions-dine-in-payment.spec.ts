import { test, expect } from '../fixtures/test-fixtures';
import {
  assignOrderTable,
  authHeaders,
  bootstrapTenant,
  createDineInOrder,
  getOrder,
  markOrderPaid,
  mintToken,
  patchKitchenStatus,
} from '../helpers/api-bootstrap';

/**
 * Companion: optional DINE_IN table, pay-anytime, payment methods, cashier deliver.
 * API-first — retries=0 friendly.
 */
test.describe('Companion — DINE_IN payment & table flexibility', () => {
  const runId = Date.now();

  test('DINE_IN without table → assign later → kitchen → deliver → cash with change', async ({
    request,
    backendApiUrl,
  }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId,
      orgPrefix: 'NoTable Org',
      productName: `Plat ${runId}`,
      productPrice: 10,
    });
    const headers = authHeaders(tenant.ownerToken);

    const tableRes = await request.post(`${backendApiUrl}/api/v1/stores/${tenant.storeId}/tables`, {
      headers,
      data: { zone: 'Terrasse', name: 'T-12', capacity: 2 },
    });
    expect(tableRes.status()).toBe(200);
    const tableId = (await tableRes.json()).data.id as string;

    const order = await createDineInOrder(request, backendApiUrl, tenant.ownerToken, {
      storeId: tenant.storeId,
      productId: tenant.productId,
      sendToKitchen: true,
    });
    expect(order.tableId == null).toBeTruthy();
    expect(order.status).toBe('SENT_TO_KITCHEN');
    expect(order.createdBy).toBe(tenant.ownerUserId);

    const assigned = await assignOrderTable(
      request,
      backendApiUrl,
      tenant.ownerToken,
      order.id,
      tableId
    );
    expect(assigned.tableId).toBe(tableId);

    await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'PREPARING');
    await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'READY');

    const deliverRes = await request.post(`${backendApiUrl}/api/v1/orders/${order.id}/deliver`, {
      headers,
    });
    expect(deliverRes.status()).toBe(200);
    expect((await deliverRes.json()).data.status).toBe('DELIVERED');

    const payment = await markOrderPaid(request, backendApiUrl, tenant.ownerToken, order.id, {
      paymentMethod: 'CASH',
      amount: order.totalAmount,
      amountTendered: Number(order.totalAmount) + 5,
    });
    expect(Number(payment.changeAmount)).toBeCloseTo(5, 2);
    expect(Number(payment.amountTendered)).toBeCloseTo(Number(order.totalAmount) + 5, 2);

    const finalRes = await getOrder(request, backendApiUrl, tenant.ownerToken, order.id);
    const finalOrder = (await finalRes.json()).data;
    expect(finalOrder.paymentStatus).toBe('PAID');
    expect(finalOrder.status).toBe('CLOSED');
  });

  test('Early CARD payment keeps kitchen status; deliver closes when already paid', async ({
    request,
    backendApiUrl,
  }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId: runId + 1,
      orgPrefix: 'EarlyPay Org',
      productName: `Burger ${runId}`,
      productPrice: 15,
    });

    const order = await createDineInOrder(request, backendApiUrl, tenant.ownerToken, {
      storeId: tenant.storeId,
      productId: tenant.productId,
      sendToKitchen: true,
    });

    await markOrderPaid(request, backendApiUrl, tenant.ownerToken, order.id, {
      paymentMethod: 'CARD',
      amount: order.totalAmount,
    });

    const afterPay = await getOrder(request, backendApiUrl, tenant.ownerToken, order.id);
    const paidOrder = (await afterPay.json()).data;
    expect(paidOrder.paymentStatus).toBe('PAID');
    expect(paidOrder.status).toBe('SENT_TO_KITCHEN');

    await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'PREPARING');
    await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'READY');

    const deliverRes = await request.post(`${backendApiUrl}/api/v1/orders/${order.id}/deliver`, {
      headers: authHeaders(tenant.ownerToken),
    });
    expect(deliverRes.status()).toBe(200);
    const delivered = (await deliverRes.json()).data;
    expect(delivered.status).toBe('CLOSED');
    expect(delivered.paymentStatus).toBe('PAID');
  });

  test('Org mobile money label + MOBILE_MONEY pay; cashier can deliver', async ({
    request,
    backendApiUrl,
  }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId: runId + 2,
      orgPrefix: 'MobilePay Org',
      productName: `Jus ${runId}`,
      productPrice: 4,
    });

    const patchOrg = await request.patch(`${backendApiUrl}/api/v1/organizations/${tenant.orgId}`, {
      headers: authHeaders(tenant.ownerToken),
      data: { mobileMoneyLabel: 'Mixx by Yas' },
    });
    expect(patchOrg.status()).toBe(200);
    expect((await patchOrg.json()).data.mobileMoneyLabel).toBe('Mixx by Yas');

    const getOrg = await request.get(`${backendApiUrl}/api/v1/organizations/${tenant.orgId}`, {
      headers: authHeaders(tenant.ownerToken),
    });
    expect(getOrg.status()).toBe(200);
    expect((await getOrg.json()).data.mobileMoneyLabel).toBe('Mixx by Yas');

    const cashierUserRes = await request.post(`${backendApiUrl}/api/v1/users`, {
      headers: authHeaders(tenant.ownerToken),
      data: {
        email: `cashier-${runId}@companion.test`,
        firstName: 'Cash',
        lastName: 'Ier',
      },
    });
    expect(cashierUserRes.status()).toBe(200);
    const cashierUserId = (await cashierUserRes.json()).data.id as string;

    await request.post(`${backendApiUrl}/api/v1/memberships`, {
      headers: authHeaders(tenant.ownerToken),
      data: { userId: cashierUserId, role: 'CASHIER', storeIds: [tenant.storeId] },
    });

    const { accessToken: cashierToken } = await mintToken(request, backendApiUrl, {
      roles: ['CASHIER'],
      organizationId: tenant.orgId,
      storeId: tenant.storeId,
      userId: cashierUserId,
    });

    const order = await createDineInOrder(request, backendApiUrl, cashierToken, {
      storeId: tenant.storeId,
      productId: tenant.productId,
      sendToKitchen: true,
    });
    expect(order.createdBy).toBe(cashierUserId);

    await markOrderPaid(request, backendApiUrl, cashierToken, order.id, {
      paymentMethod: 'MOBILE_MONEY',
      amount: order.totalAmount,
    });

    let current = (await (await getOrder(request, backendApiUrl, cashierToken, order.id)).json()).data;
    expect(current.paymentStatus).toBe('PAID');
    expect(current.status).toBe('SENT_TO_KITCHEN');

    // Kitchen still progresses after early mobile pay
    await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'PREPARING');
    await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'READY');

    // Cashier marks delivered → closes because already paid
    const deliverRes = await request.post(`${backendApiUrl}/api/v1/orders/${order.id}/deliver`, {
      headers: authHeaders(cashierToken),
    });
    expect(deliverRes.status()).toBe(200);
    current = (await deliverRes.json()).data;
    expect(current.status).toBe('CLOSED');
    expect(current.paymentStatus).toBe('PAID');
  });

  test('KDS queue exposes createdByName for cashier', async ({ request, backendApiUrl }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId: runId + 3,
      orgPrefix: 'KdsCashier Org',
      productName: `Bowl ${runId}`,
      productPrice: 9,
    });

    const order = await createDineInOrder(request, backendApiUrl, tenant.ownerToken, {
      storeId: tenant.storeId,
      productId: tenant.productId,
      sendToKitchen: true,
    });

    const queueRes = await request.get(
      `${backendApiUrl}/api/v1/kitchen/orders?storeId=${tenant.storeId}`,
      { headers: authHeaders(tenant.ownerToken) }
    );
    expect(queueRes.status()).toBe(200);
    const tickets = (await queueRes.json()).data as Array<{
      id: string;
      createdBy?: string;
      createdByName?: string;
    }>;
    const ticket = tickets.find((t) => t.id === order.id);
    expect(ticket).toBeTruthy();
    expect(ticket!.createdBy).toBe(tenant.ownerUserId);
    expect(ticket!.createdByName).toMatch(/Owner/i);
  });
});
