import { Page, Locator, expect } from '@playwright/test';

export class PosTerminalPage {
  readonly page: Page;
  readonly pinPad: Locator;
  readonly pinDisplay: Locator;
  readonly floorPlanTab: Locator;
  readonly posCatalogTab: Locator;
  readonly ordersTab: Locator;
  readonly cartItems: Locator;
  readonly cartTotal: Locator;
  readonly submitOrderBtn: Locator;
  readonly modifierModal: Locator;
  readonly modifierSubmitBtn: Locator;
  readonly markDeliveredBtn: Locator;
  readonly openPaymentBtn: Locator;
  readonly markPaidCashBtn: Locator;
  readonly payCardBtn: Locator;
  readonly payMobileBtn: Locator;
  readonly payCashToggleBtn: Locator;
  readonly cashAmountInput: Locator;
  readonly cashChangeDisplay: Locator;
  readonly clientEmailInput: Locator;
  readonly sendReceiptBtn: Locator;
  readonly orderStatusBadge: Locator;
  readonly orderWithoutTableBtn: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pinPad = page.locator('.pin-numpad');
    this.pinDisplay = page.locator('#pin-display');
    this.floorPlanTab = page.locator('#tab-floor-plan');
    this.posCatalogTab = page.locator('#tab-pos-catalog');
    this.ordersTab = page.locator('#tab-ready-orders');
    this.cartItems = page.locator('.cart-item-row');
    this.cartTotal = page.locator('#cart-total-amount');
    this.submitOrderBtn = page.locator('#submit-order-btn');
    this.modifierModal = page.locator('#modifier-selection-modal');
    this.modifierSubmitBtn = page.locator('#modifier-confirm-btn');
    this.markDeliveredBtn = page.locator('#btn-mark-delivered');
    this.openPaymentBtn = page.locator('#btn-open-payment');
    this.markPaidCashBtn = page.locator('#btn-pay-cash');
    this.payCardBtn = page.locator('#btn-pay-card');
    this.payMobileBtn = page.locator('#btn-pay-mobile');
    this.payCashToggleBtn = page.locator('#btn-pay-cash-toggle');
    this.cashAmountInput = page.locator('#cash-amount-received');
    this.cashChangeDisplay = page.locator('#cash-change-display');
    this.clientEmailInput = page.locator('#receipt-client-email');
    this.sendReceiptBtn = page.locator('#btn-send-email-receipt');
    this.orderStatusBadge = page.locator('.order-status-badge');
    this.orderWithoutTableBtn = page.locator('#btn-order-without-table');
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
    // Wait for API-driven unlock (PIN login is async), then open cash if gated
    await this.ensureCashSessionOpen();
    await expect(this.floorPlanTab).toBeVisible({ timeout: 15000 });
  }

  /**
   * Opens a cash session when the POS gate/modal is shown.
   * No-op if tabs are already available (session already open).
   */
  async ensureCashSessionOpen() {
    const confirmOpen = this.page.locator('#btn-confirm-open-cash');
    const openGateBtn = this.page.getByRole('button', { name: 'Ouvrir la caisse' });

    await expect
      .poll(
        async () => {
          if (await this.floorPlanTab.isVisible().catch(() => false)) return 'ready';
          if (await confirmOpen.isVisible().catch(() => false)) return 'modal';
          if (await openGateBtn.isVisible().catch(() => false)) return 'gate';
          return 'waiting';
        },
        { timeout: 15000 }
      )
      .not.toBe('waiting');

    if (await this.floorPlanTab.isVisible().catch(() => false)) {
      return;
    }
    if (await openGateBtn.isVisible().catch(() => false)) {
      await openGateBtn.click();
    }
    await expect(confirmOpen).toBeVisible({ timeout: 10000 });
    await confirmOpen.click();
    await expect(this.floorPlanTab).toBeVisible({ timeout: 15000 });
  }

  async selectTable(tableName: string) {
    await this.floorPlanTab.click();
    await this.page.locator(`.table-node:has-text("${tableName}")`).click();
  }

  async startOrderWithoutTable() {
    await this.floorPlanTab.click();
    await this.orderWithoutTableBtn.click();
    await expect(this.posCatalogTab).toBeVisible();
  }

  async addProductWithModifier(productName: string, modifierOptionName: string) {
    await this.posCatalogTab.click();
    await this.page.locator(`.product-card:has-text("${productName}")`).click();
    await expect(this.modifierModal).toBeVisible();
    await this.page.locator(`.modifier-option:has-text("${modifierOptionName}")`).click();
    await this.modifierSubmitBtn.click();
  }

  async addProduct(productName: string) {
    await this.posCatalogTab.click();
    await this.page.locator(`.product-card:has-text("${productName}")`).click();
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

  async openOrdersTab() {
    await this.ordersTab.click();
    await expect(this.page.locator('#ready-orders-panel')).toBeVisible();
  }

  async openOrderFromList(orderNum: string) {
    const numeric = String(orderNum).replace(/^#/, '');
    await this.openOrdersTab();
    await this.page.locator(`.ready-card[data-order-number="${numeric}"]`).click();
    await expect(this.page.locator('#order-number-display')).toContainText(`#${numeric}`);
  }

  async gotoOrder(orderNum: string) {
    const numeric = String(orderNum).replace(/^#/, '');
    await this.page.goto(`/pos/orders/${numeric}`);
    await expect(this.page.locator('#order-number-display')).toContainText(`#${numeric}`);
  }

  async assignTableOnOrder(tableName: string) {
    await this.page.locator('#order-table-select').selectOption({ label: tableName });
    await this.page.locator('#btn-assign-table').click();
  }

  async payWithCard() {
    await this.openPaymentBtn.click();
    await this.payCardBtn.click();
    await expect(this.orderStatusBadge).toContainText(/PAYÉ/);
  }

  async payWithMobile() {
    await this.openPaymentBtn.click();
    await this.payMobileBtn.click();
    await expect(this.orderStatusBadge).toContainText(/PAYÉ/);
  }

  async payWithCash(amountReceived?: number) {
    await this.openPaymentBtn.click();
    await this.payCashToggleBtn.click();
    if (amountReceived != null) {
      await this.cashAmountInput.fill(String(amountReceived));
      await expect(this.cashChangeDisplay).toBeVisible();
    }
    await this.markPaidCashBtn.click();
    await expect(this.orderStatusBadge).toContainText(/PAYÉ/);
  }

  async deliverAndPayOrder(orderNum: string, clientEmail: string) {
    await this.gotoOrder(orderNum);
    await this.markDeliveredBtn.click();
    await expect(this.orderStatusBadge).toContainText('LIVRÉ');

    await this.clientEmailInput.fill(clientEmail);
    await this.payWithCash();
  }
}
