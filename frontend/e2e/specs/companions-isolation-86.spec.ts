import { test, expect } from '../fixtures/test-fixtures';
import {
  authHeaders,
  bootstrapTenant,
  createTakeawayOrder,
  getOrder,
} from '../helpers/api-bootstrap';

/**
 * Companion: multi-org REST isolation + store-local 86 availability.
 */
test.describe('Companion — org isolation & store-local 86', () => {
  const runId = Date.now();

  test('org A cannot read org B orders; 86 is store-local only', async ({
    request,
    backendApiUrl,
  }) => {
    const orgA = await bootstrapTenant(request, backendApiUrl, {
      runId,
      orgPrefix: 'Iso Org A',
      storePrefix: 'Iso Store',
      productName: `Burger A ${runId}`,
      productPrice: 14,
      secondStore: true,
    });
    expect(orgA.storeIdB).toBeTruthy();

    const orgB = await bootstrapTenant(request, backendApiUrl, {
      runId: runId + 7,
      orgPrefix: 'Iso Org B',
      storePrefix: 'Iso Store B',
      productName: `Tacos B ${runId}`,
      productPrice: 11,
    });

    const orderB = await createTakeawayOrder(request, backendApiUrl, orgB.ownerToken, {
      storeId: orgB.storeId,
      productId: orgB.productId,
      sendToKitchen: true,
    });
    expect(orderB.status).toBe('SENT_TO_KITCHEN');

    // Org A JWT must not surface org B order (RLS → not found / bad request)
    const leakById = await getOrder(request, backendApiUrl, orgA.ownerToken, orderB.id);
    expect([400, 403, 404]).toContain(leakById.status());

    const leakList = await request.get(
      `${backendApiUrl}/api/v1/orders?storeId=${orgB.storeId}`,
      { headers: authHeaders(orgA.ownerToken) }
    );
    expect(leakList.status()).toBe(200);
    const listed = (await leakList.json()).data as Array<{ id: string }>;
    expect(listed.find((o) => o.id === orderB.id)).toBeUndefined();

    // Store-local 86 on store A only (shared product across store A + B in org A)
    const eightySix = await request.post(
      `${backendApiUrl}/api/v1/stores/${orgA.storeId}/products/${orgA.productId}/availability`,
      {
        headers: authHeaders(orgA.ownerToken),
        data: { available: false },
      }
    );
    expect(eightySix.status()).toBe(200);
    expect((await eightySix.json()).data.available).toBe(false);

    const catalogA = await request.get(
      `${backendApiUrl}/api/v1/stores/${orgA.storeId}/products`,
      { headers: authHeaders(orgA.ownerToken) }
    );
    expect(catalogA.status()).toBe(200);
    const productsA = (await catalogA.json()).data as Array<{
      productId: string;
      is86: boolean;
      available: boolean;
    }>;
    const rowA = productsA.find((p) => p.productId === orgA.productId);
    expect(rowA).toBeTruthy();
    expect(rowA!.is86).toBe(true);
    expect(rowA!.available).toBe(false);

    // Remint token bound to store B for catalog read clarity
    const catalogB = await request.get(
      `${backendApiUrl}/api/v1/stores/${orgA.storeIdB}/products`,
      { headers: authHeaders(orgA.ownerToken) }
    );
    expect(catalogB.status()).toBe(200);
    const productsB = (await catalogB.json()).data as Array<{
      productId: string;
      is86: boolean;
      available: boolean;
    }>;
    const rowB = productsB.find((p) => p.productId === orgA.productId);
    expect(rowB).toBeTruthy();
    expect(rowB!.is86).toBe(false);
    expect(rowB!.available).toBe(true);
  });
});
