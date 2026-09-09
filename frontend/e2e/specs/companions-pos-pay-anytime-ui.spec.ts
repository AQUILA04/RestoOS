import { test, expect } from '../fixtures/test-fixtures';
import {
  authHeaders,
  bootstrapTenant,
  createDineInOrder,
  getOrder,
  patchKitchenStatus,
} from '../helpers/api-bootstrap';

/**
 * UI companion: open-orders list + pay anytime + cash change + mobile label on POS.
 */
test.describe('Companion — POS open orders & pay anytime UI', () => {
  const runId = Date.now();

  test('POS Commandes: open unpaid order, pay cash with change anytime, then deliver', async ({
    request,
    backendApiUrl,
    posPage,
  }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId,
      orgPrefix: 'PosPay Org',
      productName: `Tarte ${runId}`,
      productPrice: 8,
    });

    const headers = authHeaders(tenant.ownerToken);
    const membershipRes = await request.post(`${backendApiUrl}/api/v1/memberships`, {
      headers,
      data: { userId: tenant.ownerUserId, role: 'OWNER', storeIds: [tenant.storeId] },
    });
    expect(membershipRes.status()).toBe(200);

    const pinRes = await request.post(`${backendApiUrl}/api/v1/auth/set-pin`, {
      headers,
      data: { userId: tenant.ownerUserId, pin: '4242' },
    });
    expect(pinRes.status()).toBe(200);

    const order = await createDineInOrder(request, backendApiUrl, tenant.ownerToken, {
      storeId: tenant.storeId,
      productId: tenant.productId,
      sendToKitchen: true,
    });

    await posPage.gotoPos();
    await posPage.page.evaluate(
      ([org, store, staff]) => {
        localStorage.setItem('organization_id', org as string);
        localStorage.setItem('store_id', store as string);
        localStorage.setItem('pos_staff', JSON.stringify([staff]));
      },
      [tenant.orgId, tenant.storeId, { id: tenant.ownerUserId, name: `Owner ${runId}` }]
    );
    await posPage.page.reload();
    await posPage.authenticateWithPin('4242', `Owner ${runId}`);

    await posPage.openOrderFromList(String(order.orderNumber));
    await expect(posPage.openPaymentBtn).toBeEnabled();
    await expect(posPage.markDeliveredBtn).toBeDisabled();

    const tendered = Number(order.totalAmount) + 10;
    await posPage.payWithCash(tendered);
    await expect(posPage.orderStatusBadge).toContainText(/PAYÉ/);
    await expect(posPage.page.locator('.success')).toContainText('Reliquat');

    const afterPay = await getOrder(request, backendApiUrl, tenant.ownerToken, order.id);
    expect((await afterPay.json()).data.status).toBe('SENT_TO_KITCHEN');

    await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'PREPARING');
    await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'READY');

    await posPage.gotoOrder(String(order.orderNumber));
    await expect(posPage.markDeliveredBtn).toBeEnabled();
    await posPage.markDeliveredBtn.click();
    await expect(posPage.orderStatusBadge).toContainText(/PAYÉ/);

    const finalRes = await getOrder(request, backendApiUrl, tenant.ownerToken, order.id);
    const finalOrder = (await finalRes.json()).data;
    expect(finalOrder.paymentStatus).toBe('PAID');
    expect(finalOrder.status).toBe('CLOSED');
  });

  test('Admin dashboard saves mobile money label shown on POS payment', async ({
    request,
    backendApiUrl,
    adminPage,
    posPage,
  }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId: runId + 11,
      orgPrefix: 'LabelUI Org',
      productName: `Boisson ${runId}`,
      productPrice: 3,
    });

    await adminPage.injectSession(tenant.ownerToken, tenant.orgId, tenant.storeId, tenant.ownerUserId);
    await adminPage.setMobileMoneyLabel('Flooz');

    const getOrg = await request.get(`${backendApiUrl}/api/v1/organizations/${tenant.orgId}`, {
      headers: authHeaders(tenant.ownerToken),
    });
    expect((await getOrg.json()).data.mobileMoneyLabel).toBe('Flooz');

    const order = await createDineInOrder(request, backendApiUrl, tenant.ownerToken, {
      storeId: tenant.storeId,
      productId: tenant.productId,
      sendToKitchen: true,
    });

    await posPage.page.goto('/pos');
    await posPage.page.evaluate(
      ([token, org, store, uid]) => {
        localStorage.setItem('access_token', token as string);
        localStorage.setItem('organization_id', org as string);
        localStorage.setItem('store_id', store as string);
        localStorage.setItem('user_id', uid as string);
        localStorage.setItem('resto_authenticated', 'true');
      },
      [tenant.ownerToken, tenant.orgId, tenant.storeId, tenant.ownerUserId]
    );
    await posPage.gotoOrder(String(order.orderNumber));
    await expect(posPage.page.locator('#order-number-display')).toContainText(`#${order.orderNumber}`);
    await posPage.openPaymentBtn.click();
    await expect(posPage.payMobileBtn).toContainText('Flooz');
    await posPage.payMobileBtn.click();
    await expect(posPage.orderStatusBadge).toContainText(/PAYÉ/);
  });
});
