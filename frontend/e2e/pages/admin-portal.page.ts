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

  async setStorePriceOverride(productName: string, newPrice: string) {
    await this.gotoCatalogManager();
    await this.page.locator(`tr:has-text("${productName}") #edit-price-btn`).click();
    await this.storePriceOverrideInput.fill(newPrice);
    await this.savePriceOverrideBtn.click();
    await expect(this.toastMessage).toContainText('Prix local mis à jour');
  }

  async verifyDashboardMetrics(expectedRevenue: string, minOrdersCount: number) {
    await this.gotoDashboard();
    await expect(this.dashboardRevWidget).toContainText(expectedRevenue);
    const textCount = await this.dashboardOrdersWidget.textContent();
    const count = parseInt(textCount || '0', 10);
    expect(count).toBeGreaterThanOrEqual(minOrdersCount);
  }
}
