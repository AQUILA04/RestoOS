import { randomUUID } from 'crypto';
import { test, expect } from '../fixtures/test-fixtures';
import {
  authHeaders,
  bootstrapTenant,
  createTakeawayOrder,
  getOrder,
  mintToken,
  patchKitchenStatus,
} from '../helpers/api-bootstrap';

/**
 * Companion: TAKEAWAY lifecycle + manager cancel of SENT_TO_KITCHEN.
 * API-first — exercises real JWT-backed endpoints (retries=0 friendly).
 */
test.describe('Companion — TAKEAWAY lifecycle & cancel', () => {
  const runId = Date.now();

  test('TAKEAWAY create → kitchen → READY → DELIVERED → PAID', async ({ request, backendApiUrl }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId,
      orgPrefix: 'Takeaway Org',
      productName: `Croissant ${runId}`,
      productPrice: 3.5,
    });

    const order = await createTakeawayOrder(request, backendApiUrl, tenant.ownerToken, {
      storeId: tenant.storeId,
      productId: tenant.productId,
      sendToKitchen: true,
    });
    expect(order.status).toBe('SENT_TO_KITCHEN');
    expect(order.paymentStatus).toBe('UNPAID');
    expect(order.orderNumber).not.toBe(1001);

    await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'PREPARING');
    await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'READY');

    const deliverRes = await request.post(`${backendApiUrl}/api/v1/orders/${order.id}/deliver`, {
      headers: authHeaders(tenant.ownerToken),
    });
    expect(deliverRes.status()).toBe(200);
    expect((await deliverRes.json()).data.status).toBe('DELIVERED');

    const paidRes = await request.post(
      `${backendApiUrl}/api/v1/orders/${order.id}/payment/mark-paid`,
      {
        headers: {
          ...authHeaders(tenant.ownerToken),
          'Idempotency-Key': randomUUID(),
        },
        data: {
          paymentMethod: 'CASH',
          amount: order.totalAmount,
        },
      }
    );
    expect(paidRes.status()).toBe(200);

    const finalRes = await getOrder(request, backendApiUrl, tenant.ownerToken, order.id);
    expect(finalRes.status()).toBe(200);
    const finalOrder = (await finalRes.json()).data;
    expect(finalOrder.paymentStatus).toBe('PAID');
    expect(finalOrder.status).toBe('CLOSED');
  });

  test('STORE_MANAGER cancels SENT_TO_KITCHEN with required reason', async ({
    request,
    backendApiUrl,
  }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId: runId + 1,
      orgPrefix: 'Cancel Org',
      productName: `Soup ${runId}`,
      productPrice: 8,
    });

    const managerUserRes = await request.post(`${backendApiUrl}/api/v1/users`, {
      headers: authHeaders(tenant.ownerToken),
      data: {
        email: `manager-${runId}@companion.test`,
        firstName: 'Mgr',
        lastName: `${runId}`,
      },
    });
    expect(managerUserRes.status()).toBe(200);
    const managerUserId = (await managerUserRes.json()).data.id as string;

    const { accessToken: managerToken } = await mintToken(request, backendApiUrl, {
      roles: ['STORE_MANAGER'],
      organizationId: tenant.orgId,
      storeId: tenant.storeId,
      userId: managerUserId,
    });

    const order = await createTakeawayOrder(request, backendApiUrl, managerToken, {
      storeId: tenant.storeId,
      productId: tenant.productId,
      sendToKitchen: true,
    });
    expect(order.status).toBe('SENT_TO_KITCHEN');

    const missingReason = await request.post(`${backendApiUrl}/api/v1/orders/${order.id}/cancel`, {
      headers: authHeaders(managerToken),
      data: { reason: '' },
    });
    expect(missingReason.status()).toBe(400);

    const cancelRes = await request.post(`${backendApiUrl}/api/v1/orders/${order.id}/cancel`, {
      headers: authHeaders(managerToken),
      data: { reason: `Client cancelled companion-${runId}` },
    });
    expect(cancelRes.status()).toBe(200);
    const cancelled = (await cancelRes.json()).data;
    expect(cancelled.status).toBe('CANCELLED');
    expect(cancelled.cancellationReason).toContain(`companion-${runId}`);
  });
});
