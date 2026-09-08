import { test, expect } from '../fixtures/test-fixtures';

/**
 * Golden Path — production-realistic acceptance.
 * Requires: backend (profile e2e), Keycloak or HMAC station tokens, Mailpit, Redis, Postgres app role.
 * Digital receipt email is V2 and intentionally not asserted.
 */
test.describe('RestoOS — Golden Path Full Lifecycle', () => {
  const runId = Date.now();
  const orgName = `Gourmet Bistro ${runId}`;
  const managerEmail = `manager-${runId}@bistrogourmet.fr`;
  let orgId: string;
  let storeId: string;
  let productId: string;
  let waiterUserId: string;
  let orderNumber: string;
  let ownerToken: string;

  test.beforeEach(async ({ mailpit }) => {
    await mailpit.deleteAllMessages();
  });

  test('GP: DINE_IN lifecycle with Keycloak/HMAC auth', async ({
    request,
    mailpit,
    adminPage,
    posPage,
    kdsPage,
    backendApiUrl,
  }) => {
    await test.step('Stage 0: Obtain owner token', async () => {
      if (process.env['E2E_OWNER_TOKEN']) {
        ownerToken = process.env['E2E_OWNER_TOKEN'];
        return;
      }
      const tokenRes = await request.post(`${backendApiUrl}/api/v1/test/token`, {
        data: { roles: ['OWNER'] },
      });
      expect(tokenRes.status()).toBe(200);
      ownerToken = (await tokenRes.json()).data.accessToken;
      expect(ownerToken).toBeTruthy();
    });

    await test.step('Stage 1: Organisation, store & invitation email', async () => {
      let headers: Record<string, string> = { Authorization: `Bearer ${ownerToken}` };

      const orgRes = await request.post(`${backendApiUrl}/api/v1/organizations`, {
        headers,
        data: { name: orgName, code: `ORG-${runId}` },
      });
      expect(orgRes.status()).toBe(200);
      orgId = (await orgRes.json()).data.id;

      // Re-mint owner token bound to organization
      const tokenRes = await request.post(`${backendApiUrl}/api/v1/test/token`, {
        data: { roles: ['OWNER'], organizationId: orgId },
      });
      ownerToken = (await tokenRes.json()).data.accessToken;
      headers = { Authorization: `Bearer ${ownerToken}` };

      const storeRes = await request.post(`${backendApiUrl}/api/v1/stores`, {
        headers,
        data: {
          name: 'Opera Store',
          code: `ST-${runId}`,
          timezone: 'Europe/Paris',
          currency: 'EUR',
        },
      });
      expect(storeRes.status()).toBe(200);
      storeId = (await storeRes.json()).data.id;

      // Bind store context before mutating tenant resources (matches companion bootstrap)
      const storeBoundToken = await request.post(`${backendApiUrl}/api/v1/test/token`, {
        data: { roles: ['OWNER'], organizationId: orgId, storeId },
      });
      expect(storeBoundToken.status()).toBe(200);
      ownerToken = (await storeBoundToken.json()).data.accessToken;
      headers = { Authorization: `Bearer ${ownerToken}` };

      // Persist a real OWNER user so audit_logs.user_id FK succeeds on catalog/order mutations
      const ownerUserRes = await request.post(`${backendApiUrl}/api/v1/users`, {
        headers,
        data: {
          email: `owner-${runId}@bistrogourmet.fr`,
          firstName: 'Olivier',
          lastName: 'Owner',
        },
      });
      expect(ownerUserRes.status()).toBe(200);
      const ownerUserId = (await ownerUserRes.json()).data.id as string;

      const membershipRes = await request.post(`${backendApiUrl}/api/v1/memberships`, {
        headers,
        data: { userId: ownerUserId, role: 'OWNER', storeIds: [storeId] },
      });
      expect(membershipRes.status()).toBe(200);

      const boundToken = await request.post(`${backendApiUrl}/api/v1/test/token`, {
        data: { roles: ['OWNER'], organizationId: orgId, storeId, userId: ownerUserId },
      });
      expect(boundToken.status()).toBe(200);
      ownerToken = (await boundToken.json()).data.accessToken;
      headers = { Authorization: `Bearer ${ownerToken}` };

      const inviteRes = await request.post(`${backendApiUrl}/api/v1/memberships/invite`, {
        headers,
        data: {
          email: managerEmail,
          role: 'STORE_MANAGER',
          storeId,
        },
      });
      expect(inviteRes.status()).toBe(200);

      const inviteEmail = await mailpit.waitForEmail(
        managerEmail,
        'Invitation à rejoindre RestoOS'
      );
      expect(inviteEmail.Subject).toContain('Invitation à rejoindre RestoOS');
      const html = inviteEmail.HTML || inviteEmail.Text || '';
      expect(html).toContain('Activer mon compte');
      const tokenMatch = html.match(/token=([a-f0-9-]{36})/i);
      expect(tokenMatch).toBeTruthy();
      const activateRes = await request.post(
        `${backendApiUrl}/api/v1/auth/activate?token=${tokenMatch![1]}`
      );
      if (activateRes.status() !== 200) {
        throw new Error(
          `Activation failed (${activateRes.status()}): ${await activateRes.text()}`
        );
      }
      expect(activateRes.status()).toBe(200);
    });

    await test.step('Stage 2: Catalogue, modifiers, override, table, PIN', async () => {
      const headers: Record<string, string> = ownerToken
        ? { Authorization: `Bearer ${ownerToken}` }
        : {};

      const catRes = await request.post(`${backendApiUrl}/api/v1/categories`, {
        headers,
        data: { name: 'Burgers', sortOrder: 1 },
      });
      expect(catRes.status()).toBe(200);
      const catId = (await catRes.json()).data.id;

      const prodRes = await request.post(`${backendApiUrl}/api/v1/products`, {
        headers,
        data: {
          categoryId: catId,
          name: 'Burger Signature',
          basePrice: 14.0,
          taxRate: 10,
          active: true,
        },
      });
      expect(prodRes.status()).toBe(200);
      productId = (await prodRes.json()).data.id;

      const groupRes = await request.post(`${backendApiUrl}/api/v1/catalog/modifier-groups`, {
        headers,
        data: { name: 'Cuisson', required: true, minSelection: 1, maxSelection: 1 },
      });
      expect(groupRes.status()).toBe(200);
      const groupId = (await groupRes.json()).data.id;

      const optRes = await request.post(`${backendApiUrl}/api/v1/catalog/modifier-options`, {
        headers,
        data: { modifierGroupId: groupId, name: 'A point', priceDelta: 0 },
      });
      expect(optRes.status()).toBe(200);

      const linkRes = await request.post(
        `${backendApiUrl}/api/v1/products/${productId}/modifier-groups/${groupId}`,
        { headers }
      );
      expect(linkRes.status()).toBe(200);

      const overrideRes = await request.put(
        `${backendApiUrl}/api/v1/stores/${storeId}/products/${productId}`,
        {
          headers,
          data: { priceOverride: 15.5, available: true },
        }
      );
      if (overrideRes.status() !== 200) {
        throw new Error(`Store override failed (${overrideRes.status()}): ${await overrideRes.text()}`);
      }
      expect(overrideRes.status()).toBe(200);

      // Prefer API override assertion; UI override remains covered when admin token/session present
      if (ownerToken) {
        await adminPage.page.evaluate(
          ([token, org, store]) => {
            localStorage.setItem('access_token', token as string);
            localStorage.setItem('organization_id', org as string);
            localStorage.setItem('store_id', store as string);
          },
          [ownerToken, orgId, storeId]
        );
        await adminPage.setStorePriceOverride('Burger Signature', '15.50');
      }

      const tableRes = await request.post(`${backendApiUrl}/api/v1/stores/${storeId}/tables`, {
        headers,
        data: { zone: 'Salle', name: 'Table 05', capacity: 4 },
      });
      expect(tableRes.status()).toBe(200);

      const waiterRes = await request.post(`${backendApiUrl}/api/v1/users`, {
        headers,
        data: {
          email: `waiter-${runId}@bistrogourmet.fr`,
          firstName: 'Wendy',
          lastName: 'Waiter',
        },
      });
      expect(waiterRes.status()).toBe(200);
      waiterUserId = (await waiterRes.json()).data.id;

      await request.post(`${backendApiUrl}/api/v1/memberships`, {
        headers,
        data: {
          userId: waiterUserId,
          role: 'WAITER',
          storeIds: [storeId],
        },
      });

      const pinRes = await request.post(`${backendApiUrl}/api/v1/auth/set-pin`, {
        headers,
        data: { userId: waiterUserId, pin: '1234' },
      });
      expect(pinRes.status()).toBe(200);
    });

    await test.step('Stage 3: POS PIN, table, modifiers, idempotent order', async () => {
      await posPage.page.evaluate(
        ([org, store, staff]) => {
          localStorage.setItem('organization_id', org as string);
          localStorage.setItem('store_id', store as string);
          localStorage.setItem('pos_staff', JSON.stringify(staff));
        },
        [orgId, storeId, [{ id: waiterUserId, name: 'Wendy Waiter' }]]
      );

      await posPage.authenticateWithPin('1234', 'Wendy Waiter');
      await posPage.selectTable('Table 05');
      await posPage.addProductWithModifier('Burger Signature', 'A point');
      orderNumber = await posPage.submitOrder();
      expect(orderNumber).toMatch(/#\d+/);
      expect(orderNumber).not.toBe('#1001');
    });

    await test.step('Stage 4: KDS realtime ticket → READY', async () => {
      await kdsPage.page.evaluate(
        ([token, org, store]) => {
          localStorage.setItem('access_token', token as string);
          localStorage.setItem('organization_id', org as string);
          localStorage.setItem('store_id', store as string);
        },
        [ownerToken, orgId, storeId]
      );
      await kdsPage.gotoKds();
      await kdsPage.waitForOrderTicket(orderNumber);
      await kdsPage.strikeThroughLineItem(orderNumber, 'Burger Signature');
      await kdsPage.markTicketReady(orderNumber);
    });

    await test.step('Stage 5: Deliver, pay, dashboard (no receipt V2)', async () => {
      await posPage.deliverAndPayOrder(orderNumber, `client-${runId}@gmail.com`);
      await adminPage.page.evaluate(
        ([token, org, store]) => {
          localStorage.setItem('access_token', token as string);
          localStorage.setItem('organization_id', org as string);
          localStorage.setItem('store_id', store as string);
        },
        [ownerToken, orgId, storeId]
      );
      await adminPage.verifyDashboardMetrics('15.50', 1);
    });
  });
});
