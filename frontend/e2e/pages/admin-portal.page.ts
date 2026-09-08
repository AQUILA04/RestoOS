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
    this.categoryNameInput = page.locator('#category-name');
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
    this.toastMessage = page.locator('.toast-notification');
  }

  async gotoDashboard() {
    await this.page.goto('/admin/dashboard');
  }

  async gotoCatalogManager() {
    await this.page.goto('/admin/catalog');
  }

  /** Seed JWT + tenant context used by admin dashboard / catalog pages. */
  async injectSession(token: string, organizationId: string, storeId: string) {
    await this.page.goto('/admin/dashboard');
    await this.page.evaluate(
      ([accessToken, org, store]) => {
        localStorage.setItem('access_token', accessToken as string);
        localStorage.setItem('organization_id', org as string);
        localStorage.setItem('store_id', store as string);
      },
      [token, organizationId, storeId]
    );
  }

  async setStorePriceOverride(productName: string, newPrice: string) {
    await this.gotoCatalogManager();
    const row = this.page.locator(`tr:has-text("${productName}")`);
    await expect(row).toBeVisible({ timeout: 10000 });
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
}
