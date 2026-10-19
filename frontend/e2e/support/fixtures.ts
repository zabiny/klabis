import {readFileSync} from 'node:fs';
import {test as base} from 'playwright-bdd';
import {authFile, oidcSessionFile, type Role} from './roles';

// The authorization server rotates JSESSIONID on concurrent bearer-authenticated API calls,
// so a stored cookie alone is not a reliable way back in. The OIDC user is restored into
// sessionStorage before the app boots.
type Fixtures = {
    loginAs: (role: Role) => Promise<void>;
    resolve: (text: string) => string;
};

export const test = base.extend<Fixtures>({
    // eslint-disable-next-line no-empty-pattern
    resolve: async ({}, provide, testInfo) => {
        const prefix = `E2E-${process.env.E2E_RUN_ID}-${testInfo.workerIndex}-`;
        await provide((text) => text
            .replaceAll('{prefix}', prefix)
            .replace(/\{date\+(\d+)\}/g, (_, days) => {
                const date = new Date();
                date.setDate(date.getDate() + Number(days));
                return date.toISOString().slice(0, 10);
            }));
    },
    loginAs: async ({page}, provide) => {
        await provide(async (role) => {
            const {cookies} = JSON.parse(readFileSync(authFile(role), 'utf-8'));
            const oidcEntries: [string, string][] = JSON.parse(readFileSync(oidcSessionFile(role), 'utf-8'));
            await page.context().addCookies(cookies);
            await page.addInitScript((entries) => {
                if (sessionStorage.getItem('e2e.oidcRestored')) return;
                entries.forEach(([key, value]) => sessionStorage.setItem(key, value));
                sessionStorage.setItem('e2e.oidcRestored', '1');
            }, oidcEntries);
            await page.goto('/');
            await page.locator('nav:visible').first().waitFor();
        });
    },
});
