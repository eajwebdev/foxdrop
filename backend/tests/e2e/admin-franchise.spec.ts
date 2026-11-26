import { expect, test } from '@playwright/test';
import { TOTP } from 'otpauth';

/**
 * §20.7 E2E for Phase W1: sign in → required 2FA setup → approve an operator application → record the license
 * payment offline → the province is live. Uses the demo data (Elena Ramos's application for Cebu).
 * Disputes and exports join this flow in Phase W2.
 */
const shots = process.env.FOXDROP_E2E_SHOTS;

test('a super admin sets up 2FA, approves an operator and activates the province', async ({ page }) => {
    // 1. Sign in. A fresh admin has no authenticator yet, so the setup page is the only page it can open.
    await page.goto('/admin');
    await expect(page).toHaveURL(/\/admin\/login$/);
    await page.getByLabel('Email address').fill('admin@foxdrop.local');
    await page.getByLabel('Password', { exact: true }).fill('foxdrop-local-admin');
    if (shots) await page.screenshot({ path: `${shots}/01-login.png` });
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/admin\/two-factor\/setup$/);
    await page.goto('/admin/franchises');
    await expect(page).toHaveURL(/\/admin\/two-factor\/setup$/);

    // 2. Set up the authenticator with the shown key and confirm a code.
    await page.getByRole('button', { name: 'Set up authenticator' }).click();
    await expect(page.getByTestId('2fa-secret')).toHaveText(/^[A-Z2-7]{16,}$/);
    const secret = ((await page.getByTestId('2fa-secret').textContent()) ?? '').trim();
    await expect(page.getByTestId('2fa-qr').locator('svg')).toBeVisible();
    if (shots) await page.screenshot({ path: `${shots}/02-two-factor-setup.png` });
    const code = new TOTP({ secret, digits: 6, period: 30 }).generate();
    await page.locator('input[name="code"]').fill(code);
    await page.getByRole('button', { name: 'Confirm code' }).click();
    await expect(page.getByTestId('recovery-codes')).toContainText(/\w{10}-\w{10}/);
    await page.getByRole('link', { name: 'Continue' }).click();
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByText('Operator applications').first()).toBeVisible();
    if (shots) await page.screenshot({ path: `${shots}/03-dashboard.png`, fullPage: true });

    // 3. Approve Elena Ramos's application for Cebu.
    await page.getByRole('link', { name: 'Franchises' }).first().click();
    await expect(page.getByRole('cell', { name: 'Negros Occidental' })).toBeVisible();
    if (shots) await page.screenshot({ path: `${shots}/04-franchises.png`, fullPage: true });
    await page.getByRole('link', { name: 'Operator applications' }).click();
    await page.getByRole('link', { name: 'Ramos Cebu Deliveries' }).click();
    await expect(page.getByRole('img', { name: 'Valid id' })).toBeVisible();
    if (shots) await page.screenshot({ path: `${shots}/05-application.png`, fullPage: true });
    await page.getByTestId('approve').click();
    await expect(page.getByText('Application approved')).toBeVisible();
    await expect(page.getByText('Pending payment')).toBeVisible();

    // 4. Record the bank transfer: the license becomes active and Cebu opens to customers.
    await page.getByRole('link', { name: 'Cebu', exact: true }).click();
    await page.getByRole('button', { name: 'Record payment' }).click();
    await page.getByLabel('Bank or receipt reference').fill('BDO-E2E-0001');
    if (shots) await page.screenshot({ path: `${shots}/06-record-payment.png` });
    await page.getByRole('dialog').getByRole('button', { name: 'Record payment' }).click();
    await expect(page.getByText('Payment recorded and the license updated.')).toBeVisible();
    await expect(page.getByText('Can order')).toBeVisible();
    await expect(page.getByText('Offline · BDO-E2E-0001')).toBeVisible();
    if (shots) await page.screenshot({ path: `${shots}/07-province-active.png`, fullPage: true });

    // 5. Every step is in the audit log.
    await page.getByRole('link', { name: 'Audit log' }).click();
    await expect(page.getByText('License offline payment recorded')).toBeVisible();
    await expect(page.getByText('Operator application approved')).toBeVisible();
    await expect(page.getByText('Admin two factor enabled')).toBeVisible();
    if (shots) await page.screenshot({ path: `${shots}/08-audit-log.png`, fullPage: true });
});
