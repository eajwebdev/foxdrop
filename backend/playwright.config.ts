import { defineConfig, devices } from '@playwright/test';

/**
 * Admin web end-to-end tests (§20.7). They run against a running local server with fresh demo data:
 *   php artisan migrate:fresh --seed --drop-types --drop-views && npm run build && php artisan serve --host=127.0.0.1
 * then `npm run e2e`. FOXDROP_E2E_URL points them at another server.
 */
export default defineConfig({
    testDir: 'tests/e2e',
    fullyParallel: false,
    workers: 1,
    retries: 0,
    timeout: 120_000,
    expect: { timeout: 10_000 },
    reporter: [['list'], ['html', { open: 'never' }]],
    use: {
        baseURL: process.env.FOXDROP_E2E_URL ?? 'http://127.0.0.1:8000',
        testIdAttribute: 'data-test',
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
    },
    projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
