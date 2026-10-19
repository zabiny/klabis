import {readFileSync} from 'node:fs';
import {test as base} from 'playwright-bdd';
import {authFile, oidcSessionFile, type Role} from './roles';

// The authorization server rotates JSESSIONID on concurrent bearer-authenticated API calls,
// so a stored cookie alone is not a reliable way back in. The OIDC user is restored into
// sessionStorage before the app boots.
export const test = base.extend<{ loginAs: (role: Role) => Promise<void> }>({
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
