import { Page, Locator, expect } from '@playwright/test';

export class AdminPortalPage {
  readonly page: Page;
  readonly orgNameInput: Locator;
  readonly orgSubmitBtn: Locator;
  readonly storeNameInput: Locator;
  readonly storeSubmitBtn: Locator;
  readonly categoryNameInput: Locator;
  readonly productNameInput: Locator;
  readonly productPriceInput: Locator;
  readonly storePriceOverrideInput: Locator;
  readonly savePriceOverrideBtn: Locator;
  readonly zoneInput: Locator;
  readonly tableNameInput: Locator;
  readonly tableCapacityInput: Locator;
  readonly staffPinInput: Locator;
  readonly dashboardRevWidget: Locator;
  readonly dashboardOrdersWidget: Locator;
  readonly toastMessage: Locator;

  constructor(page: Page) {
    this.page = page;
    this.orgNameInput = page.locator('#org-name');
    this.orgSubmitBtn = page.locator('#create-org-btn');
    this.storeNameInput = page.locator('#store-name');
    this.storeSubmitBtn = page.locator('#create-store-btn');
    this.categoryNameInput = page.locator('#new-category-name');
    this.productNameInput = page.locator('#product-name');
    this.productPriceInput = page.locator('#product-price');
    this.storePriceOverrideInput = page.locator('#store-price-override');
    this.savePriceOverrideBtn = page.locator('#save-override-btn');
    this.zoneInput = page.locator('#zone-name');
    this.tableNameInput = page.locator('#table-name');
    this.tableCapacityInput = page.locator('#table-capacity');
    this.staffPinInput = page.locator('#staff-pin');
    this.dashboardRevWidget = page.locator('#kpi-declared-revenue');
    this.dashboardOrdersWidget = page.locator('#kpi-total-orders');
    this.toastMessage = page.locator('#catalog-toast');
  }

  async gotoDashboard() {
    await this.page.goto('/admin/dashboard');
  }

  async gotoCatalogManager() {
    await this.page.goto('/admin/catalog');
  }

  async gotoStores() {
    await this.page.goto('/admin/etablissements');
  }

  async gotoTables() {
    await this.page.goto('/admin/tables');
  }

  async gotoUsers() {
    await this.page.goto('/admin/users');
  }

  /** Seed JWT + tenant context used by admin dashboard / catalog pages. */
  async injectSession(
    token: string,
    organizationId: string,
    storeId: string,
    userId?: string
  ) {
    await this.page.goto('/admin/dashboard', { waitUntil: 'domcontentloaded' });
    await this.page.waitForSelector('resto-root', { state: 'attached', timeout: 15000 });
    await this.page.evaluate(
      ([accessToken, org, store, uid]) => {
        localStorage.setItem('access_token', accessToken as string);
        localStorage.setItem('organization_id', org as string);
        localStorage.setItem('store_id', store as string);
        localStorage.setItem('resto_authenticated', 'true');
        if (uid) {
          localStorage.setItem('user_id', uid as string);
        }
      },
      [token, organizationId, storeId, userId || '']
    );
    await this.page.reload({ waitUntil: 'networkidle' });
    await expect(this.page.locator('h1')).toContainText('Dashboard', { timeout: 15000 });
  }

  async setStorePriceOverride(productName: string, newPrice: string) {
    const productsResponse = this.page.waitForResponse(
      (res) =>
        res.url().includes('/products') &&
        res.request().method() === 'GET' &&
        res.ok(),
      { timeout: 15000 }
    );
    await this.gotoCatalogManager();
    await expect(this.page.locator('h1')).toContainText('Catalogue', { timeout: 15000 });
    await productsResponse;
    const row = this.page.locator(`tr:has-text("${productName}")`);
    await expect(row).toBeVisible({ timeout: 15000 });
    await row.locator('#edit-price-btn').click();
    await this.storePriceOverrideInput.fill(newPrice);
    await this.savePriceOverrideBtn.click();
    await expect(this.toastMessage).toContainText('Prix local mis à jour');
  }

  async verifyDashboardMetrics(expectedRevenue: string, ordersCount: number) {
    await this.gotoDashboard();
    await expect(this.dashboardRevWidget).toContainText(expectedRevenue);
    const textCount = await this.dashboardOrdersWidget.textContent();
    const count = parseInt(textCount || '0', 10);
    expect(count).toBe(ordersCount);
  }

  /**
   * Exact KPI assertions after session inject (no soft >= thresholds).
   * Reloads dashboard so metrics fetch with the seeded store_id.
   */
  async verifyDashboardMetricsExact(
    token: string,
    organizationId: string,
    storeId: string,
    expectedRevenue: string,
    ordersCount: number
  ) {
    await this.injectSession(token, organizationId, storeId);
    await this.page.reload();
    await this.verifyDashboardMetrics(expectedRevenue, ordersCount);
  }

  async setMobileMoneyLabel(label: string) {
    await this.gotoDashboard();
    await expect(this.page.locator('#org-payment-settings')).toBeVisible({ timeout: 15000 });
    await this.page.locator('#mobile-money-label-input').fill(label);
    await this.page.locator('#btn-save-mobile-money-label').click();
    await expect(this.page.locator('.toast')).toContainText('Libellé enregistré', { timeout: 10000 });
  }

  async saveTenantBranding(opts: { name?: string; logoUrl?: string; mobileMoneyLabel?: string }) {
    await this.gotoDashboard();
    await expect(this.page.locator('#org-payment-settings')).toBeVisible({ timeout: 15000 });
    if (opts.name !== undefined) {
      await this.page.locator('#org-name-input').fill(opts.name);
    }
    if (opts.logoUrl !== undefined) {
      await this.page.locator('#org-logo-url-input').fill(opts.logoUrl);
    }
    if (opts.mobileMoneyLabel !== undefined) {
      await this.page.locator('#mobile-money-label-input').fill(opts.mobileMoneyLabel);
    }
    await this.page.locator('#btn-save-mobile-money-label').click();
    await expect(this.page.locator('.toast')).toContainText('Libellé enregistré', { timeout: 10000 });
  }

  async updateStoreSettings(storeId: string, opts: { name?: string; currency?: string; timezone?: string }) {
    await this.gotoStores();
    await expect(this.page.locator('#store-list')).toBeVisible({ timeout: 15000 });
    if (opts.name !== undefined) {
      await this.page.locator(`#store-name-${storeId}`).fill(opts.name);
    }
    if (opts.currency !== undefined) {
      await this.page.locator(`#store-currency-${storeId}`).selectOption(opts.currency);
    }
    if (opts.timezone !== undefined) {
      await this.page.locator(`#store-tz-${storeId}`).fill(opts.timezone);
    }
    await this.page.locator(`[data-testid="rename-${storeId}"]`).click();
    await expect(this.page.locator('#stores-message')).toContainText(/mis à jour/i, { timeout: 10000 });
  }

  async addCategory(name: string) {
    await this.gotoCatalogManager();
    await expect(this.page.locator('h1')).toContainText('Catalogue', { timeout: 15000 });
    await this.categoryNameInput.fill(name);
    await this.page.locator('#btn-add-category').click();
    await expect(this.toastMessage).toContainText('Catégorie créée', { timeout: 10000 });
    await expect(this.page.locator('#categories-panel')).toContainText(name);
  }

  async createProduct(opts: {
    name: string;
    price: string;
    prepMinutes: string;
    description?: string;
  }) {
    await this.gotoCatalogManager();
    await expect(this.page.locator('h1')).toContainText('Catalogue', { timeout: 15000 });
    await this.page.locator('#btn-new-product').click();
    await expect(this.page.locator('#product-form-panel')).toBeVisible();
    await this.productNameInput.fill(opts.name);
    await this.productPriceInput.fill(opts.price);
    await this.page.locator('#product-prep').fill(opts.prepMinutes);
    if (opts.description) {
      await this.page.locator('#product-desc').fill(opts.description);
    }
    await this.page.locator('#btn-save-product').click();
    await expect(this.toastMessage).toContainText(/Produit (créé|mis à jour)/, { timeout: 10000 });
    const row = this.page.locator(`tr:has-text("${opts.name}")`);
    await expect(row).toBeVisible({ timeout: 15000 });
    await expect(row).toContainText(`${opts.prepMinutes} min`);
  }

  async addZone(name: string) {
    await this.gotoTables();
    await expect(this.page.locator('h1')).toContainText('Tables', { timeout: 15000 });
    await this.zoneInput.fill(name);
    await this.page.locator('#btn-add-zone').click();
    await expect(this.page.locator('#tables-toast')).toContainText('Zone créée', { timeout: 10000 });
    await expect(this.page.locator('.zone-list')).toContainText(name);
  }

  async addTable(opts: { name: string; capacity: string }) {
    await this.gotoTables();
    await expect(this.page.locator('h1')).toContainText('Tables', { timeout: 15000 });
    await this.tableNameInput.fill(opts.name);
    await this.tableCapacityInput.fill(opts.capacity);
    await this.page.locator('#btn-add-table').click();
    await expect(this.page.locator('#tables-toast')).toContainText('Table créée', { timeout: 10000 });
    await expect(this.page.locator(`tr:has-text("${opts.name}")`)).toBeVisible({ timeout: 10000 });
  }

  async inviteStaff(opts: { email: string; role: string }) {
    await this.gotoUsers();
    await expect(this.page.locator('h1')).toContainText('Utilisateurs', { timeout: 15000 });
    await this.page.locator('#invite-email').fill(opts.email);
    await this.page.locator('#invite-role').selectOption(opts.role);
    await this.page.locator('#btn-invite-user').click();
    await expect(this.page.locator('#users-toast')).toContainText('Invitation envoyée', { timeout: 10000 });
    await expect(this.page.locator(`tr:has-text("${opts.email}")`)).toBeVisible({ timeout: 10000 });
  }

  async setMemberRole(email: string, role: string) {
    await this.gotoUsers();
    const row = this.page.locator(`tr:has-text("${email}")`);
    await expect(row).toBeVisible({ timeout: 15000 });
    await row.locator('select').first().selectOption(role);
    await row.getByRole('button', { name: 'OK' }).click();
    await expect(this.page.locator('#users-toast')).toContainText('Rôle mis à jour', { timeout: 10000 });
  }

  async setMemberPin(email: string, pin: string) {
    await this.gotoUsers();
    const row = this.page.locator(`tr:has-text("${email}")`);
    await expect(row).toBeVisible({ timeout: 15000 });
    await row.locator('input[type="password"]').fill(pin);
    await row.getByRole('button', { name: 'PIN' }).click();
    await expect(this.page.locator('#users-toast')).toContainText('PIN enregistré', { timeout: 10000 });
    await expect(row).toContainText('Défini');
  }
}
