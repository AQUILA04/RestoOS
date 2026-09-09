import { test, expect } from '@playwright/test';

/**
 * UI onboarding: landing → signup → default store → rename → add second store.
 * Uses HMAC session from signup (e2e profile); Keycloak bounce is covered lightly via /login redirect URL.
 */
test.describe('Onboarding self-serve', () => {
  test('signup creates default establishment, rename and multi-store', async ({ page, baseURL }) => {
    const runId = Date.now();
    const orgName = `Brasserie ${runId}`;
    const email = `owner-${runId}@onboard.test`;
    const password = 'OnboardPass123!';

    await page.goto('/');
    await expect(page.getByRole('heading', { name: /La salle, la cuisine, le service/i })).toBeVisible();
    await page.locator('#cta-create-space').click();
    await expect(page).toHaveURL(/\/signup/);

    await page.locator('#org-name').fill(orgName);
    await page.locator('#first-name').fill('Olivia');
    await page.locator('#last-name').fill('Owner');
    await page.locator('#email').fill(email);
    await page.locator('#password').fill(password);
    await page.locator('input[name="storeMode"][value="single"]').check();
    await page.locator('#signup-submit').click();

    await expect(page).toHaveURL(/\/admin\/dashboard/, { timeout: 20_000 });
    expect(await page.evaluate(() => localStorage.getItem('access_token'))).toBeTruthy();
    expect(await page.evaluate(() => localStorage.getItem('organization_id'))).toBeTruthy();
    const storeId = await page.evaluate(() => localStorage.getItem('store_id'));
    expect(storeId).toBeTruthy();

    await page.goto('/admin/etablissements');
    await expect(page.locator('#store-list li')).toHaveCount(1);
    const nameInput = page.locator('#store-list input').first();
    await expect(nameInput).toHaveValue(orgName);

    const renamed = `${orgName} Centre`;
    await nameInput.fill(renamed);
    await page.locator('#store-list button').filter({ hasText: 'Enregistrer' }).first().click();
    await expect(page.locator('#stores-message')).toContainText(/mis à jour/i);
    await expect(nameInput).toHaveValue(renamed);

    await page.locator('#new-store-name').fill(`${orgName} Gare`);
    await page.locator('#btn-add-store').click();
    await expect(page.locator('#store-list li')).toHaveCount(2, { timeout: 15_000 });

    await page.locator('#store-list button').filter({ hasText: 'Activer' }).nth(1).click();
    await expect(page.locator('#stores-message')).toContainText(/Restaurant actif/i);
  });

  test('duplicate email is rejected', async ({ page }) => {
    const runId = Date.now();
    const email = `dup-${runId}@onboard.test`;
    const password = 'OnboardPass123!';

    await page.goto('/signup');
    await page.locator('#org-name').fill(`Org A ${runId}`);
    await page.locator('#email').fill(email);
    await page.locator('#password').fill(password);
    await page.locator('#signup-submit').click();
    await expect(page).toHaveURL(/\/admin\//, { timeout: 20_000 });

    await page.evaluate(() => localStorage.clear());
    await page.goto('/signup');
    await page.locator('#org-name').fill(`Org B ${runId}`);
    await page.locator('#email').fill(email);
    await page.locator('#password').fill(password);
    await page.locator('#signup-submit').click();
    await expect(page.locator('#signup-error')).toContainText(/déjà utilisé/i);
  });

  test('Se connecter builds Keycloak authorize URL', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#cta-connect')).toBeVisible();
    const [req] = await Promise.all([
      page.waitForRequest((r) =>
        /\/realms\/restoos\/protocol\/openid-connect\/auth/.test(r.url()),
      ),
      page.locator('#cta-connect').click(),
    ]);
    expect(req.url()).toContain('client_id=restoos-frontend');
    expect(req.url()).toContain('code_challenge');
    expect(req.url()).toContain('redirect_uri=');
  });
});
