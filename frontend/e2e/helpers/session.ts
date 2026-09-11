import { Page } from '@playwright/test';

/** Seed browser session on a public route so Angular auth guards do not bounce before storage is set. */
export async function seedBrowserSession(
  page: Page,
  session: {
    accessToken?: string;
    organizationId: string;
    storeId: string;
    userId?: string;
    staff?: unknown;
  }
): Promise<void> {
  await page.goto('/', { waitUntil: 'domcontentloaded' });
  await page.evaluate(
    ([accessToken, org, store, uid, staff]) => {
      [
        'access_token',
        'user_id',
        'organization_id',
        'store_id',
        'roles',
        'resto_authenticated',
        'pos_staff',
        'resto_org_id',
        'resto_store_id',
        'resto_user_id',
      ].forEach((k) => localStorage.removeItem(k));

      if (accessToken) {
        localStorage.setItem('access_token', accessToken as string);
      }
      localStorage.setItem('organization_id', org as string);
      localStorage.setItem('store_id', store as string);
      if (uid) {
        localStorage.setItem('user_id', uid as string);
        localStorage.setItem('resto_authenticated', 'true');
      }
      if (staff) {
        localStorage.setItem('pos_staff', JSON.stringify(staff));
      }
    },
    [
      session.accessToken || '',
      session.organizationId,
      session.storeId,
      session.userId || '',
      session.staff ?? null,
    ]
  );
}
