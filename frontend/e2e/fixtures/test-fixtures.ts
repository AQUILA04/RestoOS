import { test as base, Browser, BrowserContext, Page } from '@playwright/test';
import { MailpitClient } from '../helpers/mailpit.client';
import { AdminPortalPage } from '../pages/admin-portal.page';
import { PosTerminalPage } from '../pages/pos-terminal.page';
import { KdsScreenPage } from '../pages/kds-screen.page';

export type RestoFixtures = {
  mailpit: MailpitClient;
  adminPage: AdminPortalPage;
  posPage: PosTerminalPage;
  kdsPage: KdsScreenPage;
  backendApiUrl: string;
  ownerContext: BrowserContext;
  posContext: BrowserContext;
  kdsContext: BrowserContext;
};

async function newRoleContext(browser: Browser): Promise<BrowserContext> {
  return browser.newContext({
    baseURL: process.env['BASE_URL'] || 'http://localhost:4200',
  });
}

export const test = base.extend<RestoFixtures>({
  backendApiUrl: [process.env['API_URL'] || 'http://localhost:8080', { option: true }],

  mailpit: async ({}, use) => {
    const mailpit = new MailpitClient();
    await mailpit.deleteAllMessages();
    await use(mailpit);
  },

  ownerContext: async ({ browser }, use) => {
    const ctx = await newRoleContext(browser);
    await use(ctx);
    await ctx.close();
  },

  posContext: async ({ browser }, use) => {
    const ctx = await newRoleContext(browser);
    await use(ctx);
    await ctx.close();
  },

  kdsContext: async ({ browser }, use) => {
    const ctx = await newRoleContext(browser);
    await use(ctx);
    await ctx.close();
  },

  adminPage: async ({ ownerContext }, use) => {
    const page: Page = await ownerContext.newPage();
    await use(new AdminPortalPage(page));
  },

  posPage: async ({ posContext }, use) => {
    const page: Page = await posContext.newPage();
    await use(new PosTerminalPage(page));
  },

  kdsPage: async ({ kdsContext }, use) => {
    const page: Page = await kdsContext.newPage();
    await use(new KdsScreenPage(page));
  },
});

export { expect } from '@playwright/test';
