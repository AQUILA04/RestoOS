import { Page, Locator, expect } from '@playwright/test';

export class KdsScreenPage {
  readonly page: Page;
  readonly kdsContainer: Locator;
  readonly activeTickets: Locator;

  constructor(page: Page) {
    this.page = page;
    this.kdsContainer = page.locator('.kds-obsidian-grid');
    this.activeTickets = page.locator('.kds-ticket-card');
  }

  async gotoKds() {
    await this.page.goto('/kds');
    await expect(this.kdsContainer).toBeVisible();
  }

  async waitForOrderTicket(orderNum: string): Promise<Locator> {
    const ticket = this.page.locator(`.kds-ticket-card:has-text("${orderNum}")`);
    await expect(ticket).toBeVisible({ timeout: 10000 });
    return ticket;
  }

  async strikeThroughLineItem(orderNum: string, itemText: string) {
    const ticket = await this.waitForOrderTicket(orderNum);
    const itemRow = ticket.locator(`.ticket-item:has-text("${itemText}")`);
    await itemRow.click();
    await expect(itemRow).toHaveClass(/item-done-strikethrough/);
  }

  async markTicketReady(orderNum: string) {
    const ticket = await this.waitForOrderTicket(orderNum);
    const readyBtn = ticket.locator('#btn-ticket-ready');
    await readyBtn.click();
    await expect(ticket).not.toBeVisible();
  }
}
