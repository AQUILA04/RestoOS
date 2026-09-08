import { test as base } from '@playwright/test';
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
};

export const test = base.extend<RestoFixtures>({
  backendApiUrl: [process.env['API_URL'] || 'http://localhost:8080', { option: true }],

  mailpit: async ({}, use) => {
    const mailpit = new MailpitClient();
    await mailpit.deleteAllMessages();
    await use(mailpit);
  },

  adminPage: async ({ page }, use) => {
    const adminPage = new AdminPortalPage(page);
    await use(adminPage);
  },

  posPage: async ({ page }, use) => {
    const posPage = new PosTerminalPage(page);
    await use(posPage);
  },

  kdsPage: async ({ page }, use) => {
    const kdsPage = new KdsScreenPage(page);
    await use(kdsPage);
  },
});

export { expect } from '@playwright/test';
