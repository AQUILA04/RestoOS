import { Page, Locator, expect } from '@playwright/test';

export class PosTerminalPage {
  readonly page: Page;
  readonly pinPad: Locator;
  readonly pinDisplay: Locator;
  readonly floorPlanTab: Locator;
  readonly posCatalogTab: Locator;
  readonly cartItems: Locator;
  readonly cartTotal: Locator;
  readonly submitOrderBtn: Locator;
  readonly modifierModal: Locator;
  readonly modifierSubmitBtn: Locator;
  readonly markDeliveredBtn: Locator;
  readonly markPaidCashBtn: Locator;
  readonly clientEmailInput: Locator;
  readonly sendReceiptBtn: Locator;
  readonly orderStatusBadge: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pinPad = page.locator('.pin-numpad');
    this.pinDisplay = page.locator('#pin-display');
    this.floorPlanTab = page.locator('#tab-floor-plan');
    this.posCatalogTab = page.locator('#tab-pos-catalog');
    this.cartItems = page.locator('.cart-item-row');
    this.cartTotal = page.locator('#cart-total-amount');
    this.submitOrderBtn = page.locator('#submit-order-btn');
    this.modifierModal = page.locator('#modifier-selection-modal');
    this.modifierSubmitBtn = page.locator('#modifier-confirm-btn');
    this.markDeliveredBtn = page.locator('#btn-mark-delivered');
    this.markPaidCashBtn = page.locator('#btn-pay-cash');
    this.clientEmailInput = page.locator('#receipt-client-email');
    this.sendReceiptBtn = page.locator('#btn-send-email-receipt');
    this.orderStatusBadge = page.locator('.order-status-badge');
  }

  async gotoPos() {
    await this.page.goto('/pos');
  }

  async authenticateWithPin(pin: string, userName?: string) {
    await this.gotoPos();
    if (userName) {
      await this.page.locator(`.user-card:has-text("${userName}")`).click();
    } else {
      await this.page.locator('.user-card').first().click();
    }
    for (const digit of pin) {
      await this.page.locator(`.pin-button[data-digit="${digit}"]`).click();
    }
    await expect(this.pinDisplay).toHaveText('••••');
    await expect(this.floorPlanTab).toBeVisible({ timeout: 5000 });
  }

  async selectTable(tableName: string) {
    await this.floorPlanTab.click();
    await this.page.locator(`.table-node:has-text("${tableName}")`).click();
  }

  async addProductWithModifier(productName: string, modifierOptionName: string) {
    await this.posCatalogTab.click();
    await this.page.locator(`.product-card:has-text("${productName}")`).click();
    await expect(this.modifierModal).toBeVisible();
    await this.page.locator(`.modifier-option:has-text("${modifierOptionName}")`).click();
    await this.modifierSubmitBtn.click();
  }

  async submitOrder(): Promise<string> {
    await this.submitOrderBtn.click();
    await expect(this.orderStatusBadge).toContainText('EN CUISINE');
    const orderNum = await this.page.locator('#order-number-display').textContent();
    if (!orderNum || !orderNum.trim()) {
      throw new Error('Order number display missing after submit');
    }
    return orderNum.trim();
  }

  async deliverAndPayOrder(orderNum: string, clientEmail: string) {
    await this.page.goto(`/pos/orders/${orderNum}`);
    await this.markDeliveredBtn.click();
    await expect(this.orderStatusBadge).toContainText('LIVRÉ');

    await this.clientEmailInput.fill(clientEmail);
    await this.markPaidCashBtn.click();
    await expect(this.orderStatusBadge).toContainText('PAYÉ');
  }
}
