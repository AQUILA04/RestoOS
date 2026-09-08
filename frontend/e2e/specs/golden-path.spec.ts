import { test, expect } from '../fixtures/test-fixtures';

test.describe('RestoOS — Golden Path Full Lifecycle E2E Suite', () => {
  const orgName = `Gourmet Bistro ${Date.now()}`;
  const managerEmail = `manager-${Date.now()}@bistrogourmet.fr`;
  const clientEmail = `client-${Date.now()}@gmail.com`;
  let orgId: string;
  let storeId: string;
  let orderNumber: string;

  test.beforeEach(async ({ mailpit }) => {
    // Ensure clean mailpit state before execution
    await mailpit.deleteAllMessages();
  });

  test('GP-01 to GP-05: Complete Golden Path Lifecycle', async ({
    request,
    mailpit,
    adminPage,
    posPage,
    kdsPage,
    backendApiUrl,
  }) => {
    // =========================================================================
    // STAGE 1: Provisioning Organisation, Store & Verification Email Mailpit
    // =========================================================================
    await test.step('Stage 1: Organisation creation & Mailpit invitation assertion', async () => {
      // 1. Create Organization
      const orgRes = await request.post(`${backendApiUrl}/api/v1/organizations`, {
        data: { name: orgName, country: 'FR', currency: 'EUR' },
      });
      expect(orgRes.status()).toBe(200);
      const orgData = await orgRes.json();
      orgId = orgData.data.id;
      expect(orgId).toBeDefined();

      // 2. Create Store
      const storeRes = await request.post(`${backendApiUrl}/api/v1/stores`, {
        headers: { 'X-Tenant-ID': orgId },
        data: { name: 'Opera Store', city: 'Paris', timezone: 'Europe/Paris' },
      });
      expect(storeRes.status()).toBe(200);
      const storeData = await storeRes.json();
      storeId = storeData.data.id;
      expect(storeId).toBeDefined();

      // 3. Invite Manager (Triggers Email via SMTP Mailpit)
      const inviteRes = await request.post(`${backendApiUrl}/api/v1/memberships/invite`, {
        headers: { 'X-Tenant-ID': orgId },
        data: {
          email: managerEmail,
          role: 'STORE_MANAGER',
          storeId: storeId,
        },
      });
      expect(inviteRes.status()).toBe(200);

      // 4. Assert Mailpit intercepted invitation email
      const inviteEmail = await mailpit.waitForEmail(
        managerEmail,
        'Invitation à rejoindre RestoOS'
      );
      expect(inviteEmail.Subject).toContain('Invitation à rejoindre RestoOS');
      expect(inviteEmail.HTML).toContain('Activer mon compte');
    });

    // =========================================================================
    // STAGE 2: Catalogue Setup, Price Overrides & Floor Plan Configuration
    // =========================================================================
    await test.step('Stage 2: Operational catalog, store price override & table setup', async () => {
      // 1. Create Category
      const catRes = await request.post(`${backendApiUrl}/api/v1/categories`, {
        headers: { 'X-Tenant-ID': orgId },
        data: { name: 'Burgers', sortOrder: 1 },
      });
      const catId = (await catRes.json()).data.id;

      // 2. Create Product (Base Price 14.00 €)
      const prodRes = await request.post(`${backendApiUrl}/api/v1/products`, {
        headers: { 'X-Tenant-ID': orgId },
        data: {
          categoryId: catId,
          name: 'Burger Signature',
          basePrice: 14.0,
          active: true,
        },
      });
      const prodId = (await prodRes.json()).data.id;

      // 3. Store Price Override (15.50 €)
      await adminPage.setStorePriceOverride('Burger Signature', '15.50');

      // 4. Create Zone & Table 05
      const tableRes = await request.post(`${backendApiUrl}/api/v1/stores/${storeId}/tables`, {
        headers: { 'X-Tenant-ID': orgId },
        data: { zone: 'Salle', name: 'Table 05', capacity: 4 },
      });
      expect(tableRes.status()).toBe(200);
    });

    // =========================================================================
    // STAGE 3: POS Touch Terminal & Order Idempotency Assembly
    // =========================================================================
    await test.step('Stage 3: Waiter PIN Auth, Table Selection & POS Order Submission', async () => {
      // 1. PIN Lockscreen Authentication (PIN: 1234)
      await posPage.authenticateWithPin('1234');

      // 2. Select Table 05 on Floor Plan
      await posPage.selectTable('Table 05');

      // 3. Add Product with Required Modifier "A point"
      await posPage.addProductWithModifier('Burger Signature', 'A point');

      // 4. Submit Order (Backend price recalculation to 15.50 € + Idempotency check)
      orderNumber = await posPage.submitOrder();
      expect(orderNumber).toBeDefined();
    });

    // =========================================================================
    // STAGE 4: Real-Time KDS STOMP Dispatcher & Kitchen Progression
    // =========================================================================
    await test.step('Stage 4: Real-time KDS STOMP ticket receipt & preparation state', async () => {
      await kdsPage.gotoKds();
      await kdsPage.waitForOrderTicket(orderNumber);
      await kdsPage.strikeThroughLineItem(orderNumber, 'Burger Signature');
      await kdsPage.markTicketReady(orderNumber);
    });

    // =========================================================================
    // STAGE 5: Order Delivery, Declarative Payment, Mailpit Digital Receipt & Audit
    // =========================================================================
    await test.step('Stage 5: Order delivery, payment mark-paid & Mailpit receipt assertion', async () => {
      // 1. Deliver & Mark Paid (Cash 15.50 €)
      await posPage.deliverAndPayOrder(orderNumber, clientEmail);

      // 2. Assert Mailpit received digital email receipt
      const receiptEmail = await mailpit.waitForEmail(
        clientEmail,
        'Votre Reçu RestoOS'
      );
      expect(receiptEmail.Subject).toContain('Votre Reçu RestoOS');
      expect(receiptEmail.HTML).toContain('15.50');

      // 3. Verify Operational KPI Dashboard Metrics
      await adminPage.verifyDashboardMetrics('15.50', 1);
    });
  });
});
