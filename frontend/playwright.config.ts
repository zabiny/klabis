import {defineConfig, devices} from '@playwright/test';
import {defineBddConfig} from 'playwright-bdd';

process.env.E2E_RUN_ID ??= Date.now().toString(36);

const testDir = defineBddConfig({
    features: 'e2e/features/**/*.feature',
    steps: ['e2e/steps/**/*.ts', 'e2e/support/fixtures.ts', 'e2e/support/parameters.ts'],
});

export default defineConfig({
    testDir: 'e2e',
    reporter: [['html', {open: 'never'}], ['list']],
    // Shared per-role sessions and backend JSESSIONID rotation make parallel workers unreliable.
    workers: 1,
    use: {
        baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
        ignoreHTTPSErrors: true,
        trace: 'retain-on-failure',
    },
    projects: [
        {name: 'setup', testMatch: /auth\.setup\.ts/},
        {
            name: 'chromium',
            testDir,
            use: {...devices['Desktop Chrome']},
            dependencies: ['setup'],
        },
    ],
});
