import { test, expect } from '../fixtures/test-fixtures';
import { authHeaders, bootstrapTenant } from '../helpers/api-bootstrap';

/**
 * UI coverage for owner tenant administration:
 * branding, currency, full menu, floor plan, team invite/role/PIN.
 */
test.describe('Owner admin console UI', () => {
  const runId = Date.now();

  test('tenant branding: name + logo URL', async ({ request, backendApiUrl, adminPage }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId: runId + 1,
      orgPrefix: 'Brand Org',
      productName: `Brand Item ${runId}`,
    });

    await adminPage.injectSession(tenant.ownerToken, tenant.orgId, tenant.storeId, tenant.ownerUserId);

    const newName = `Enseigne ${runId}`;
    const logoUrl = 'https://cdn.example.com/logos/resto.png';
    await adminPage.saveTenantBranding({
      name: newName,
      logoUrl,
      mobileMoneyLabel: 'Mixx by Yas',
    });

    const orgRes = await request.get(`${backendApiUrl}/api/v1/organizations/${tenant.orgId}`, {
      headers: authHeaders(tenant.ownerToken),
    });
    expect(orgRes.status()).toBe(200);
    const org = (await orgRes.json()).data;
    expect(org.name).toBe(newName);
    expect(org.logoUrl).toBe(logoUrl);
    expect(org.mobileMoneyLabel).toBe('Mixx by Yas');
  });

  test('store currency and timezone via établissements', async ({
    request,
    backendApiUrl,
    adminPage,
  }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId: runId + 2,
      orgPrefix: 'Currency Org',
      productName: `Cur Item ${runId}`,
    });

    await adminPage.injectSession(tenant.ownerToken, tenant.orgId, tenant.storeId, tenant.ownerUserId);
    await adminPage.updateStoreSettings(tenant.storeId, {
      name: `Boutique XOF ${runId}`,
      currency: 'XOF',
      timezone: 'Africa/Lome',
    });

    const storesRes = await request.get(`${backendApiUrl}/api/v1/stores`, {
      headers: authHeaders(tenant.ownerToken),
    });
    expect(storesRes.status()).toBe(200);
    const store = ((await storesRes.json()).data as Array<Record<string, string>>).find(
      (s) => s.id === tenant.storeId
    );
    expect(store).toBeTruthy();
    expect(store!.currency).toBe('XOF');
    expect(store!.timezone).toBe('Africa/Lome');
    expect(store!.name).toContain('Boutique XOF');
  });

  test('catalog UI: category + product with average prep time', async ({
    request,
    backendApiUrl,
    adminPage,
  }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId: runId + 3,
      orgPrefix: 'Menu Org',
      productName: `Seed Plat ${runId}`,
    });

    await adminPage.injectSession(tenant.ownerToken, tenant.orgId, tenant.storeId, tenant.ownerUserId);

    const categoryName = `Desserts ${runId}`;
    await adminPage.addCategory(categoryName);

    const productName = `Tiramisu ${runId}`;
    await adminPage.createProduct({
      name: productName,
      price: '6.50',
      prepMinutes: '12',
      description: 'Maison',
    });

    const productsRes = await request.get(`${backendApiUrl}/api/v1/products`, {
      headers: authHeaders(tenant.ownerToken),
    });
    expect(productsRes.status()).toBe(200);
    const products = (await productsRes.json()).data as Array<{
      name: string;
      avgPrepMinutes: number;
      basePrice: number;
    }>;
    const created = products.find((p) => p.name === productName);
    expect(created).toBeTruthy();
    expect(created!.avgPrepMinutes).toBe(12);
    expect(Number(created!.basePrice)).toBeCloseTo(6.5, 2);
  });

  test('tables UI: create zone and table', async ({ request, backendApiUrl, adminPage }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId: runId + 4,
      orgPrefix: 'Floor Org',
      productName: `Floor Item ${runId}`,
    });

    await adminPage.injectSession(tenant.ownerToken, tenant.orgId, tenant.storeId, tenant.ownerUserId);

    const zoneName = `Terrasse ${runId}`;
    await adminPage.addZone(zoneName);

    const tableName = `T-${runId % 10000}`;
    await adminPage.addTable({ name: tableName, capacity: '6' });

    const tablesRes = await request.get(
      `${backendApiUrl}/api/v1/stores/${tenant.storeId}/tables`,
      { headers: authHeaders(tenant.ownerToken) }
    );
    expect(tablesRes.status()).toBe(200);
    const tables = (await tablesRes.json()).data as Array<{ tableNumber: string; capacity: number }>;
    const table = tables.find((t) => t.tableNumber === tableName);
    expect(table).toBeTruthy();
    expect(table!.capacity).toBe(6);
  });

  test('team UI: invite, change role, set PIN', async ({
    request,
    backendApiUrl,
    adminPage,
    mailpit,
  }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId: runId + 5,
      orgPrefix: 'Team Org',
      productName: `Team Item ${runId}`,
    });

    // Owner membership so store select is populated and list APIs resolve cleanly
    const membershipRes = await request.post(`${backendApiUrl}/api/v1/memberships`, {
      headers: authHeaders(tenant.ownerToken),
      data: { userId: tenant.ownerUserId, role: 'OWNER', storeIds: [tenant.storeId] },
    });
    expect(membershipRes.status()).toBe(200);

    await adminPage.injectSession(tenant.ownerToken, tenant.orgId, tenant.storeId, tenant.ownerUserId);

    const email = `waiter-${runId}@team.test`;
    await adminPage.inviteStaff({ email, role: 'WAITER' });

    const inviteMail = await mailpit.waitForEmail(email, 'Invitation', 15000);
    expect(inviteMail.Subject).toContain('Invitation');

    await adminPage.setMemberRole(email, 'CASHIER');
    await adminPage.setMemberPin(email, '9876');

    const membersRes = await request.get(`${backendApiUrl}/api/v1/memberships`, {
      headers: authHeaders(tenant.ownerToken),
    });
    expect(membersRes.status()).toBe(200);
    const members = (await membersRes.json()).data as Array<{
      email: string;
      role: string;
      hasPin: boolean;
    }>;
    const invited = members.find((m) => m.email === email);
    expect(invited).toBeTruthy();
    expect(invited!.role).toBe('CASHIER');
    expect(invited!.hasPin).toBe(true);
  });
});
