import {mkdirSync, writeFileSync} from 'node:fs';
import {dirname} from 'node:path';
import {expect, type Browser, type Page} from '@playwright/test';
import {waitForApp} from './app';
import {type Role, roles, type RoleSession, sessionFile} from './roles';

export const loginAndStoreSession = async (page: Page, role: Role) => {
    await page.goto('/');
    await page.getByPlaceholder('např. 12345').fill(roles[role].user);
    await page.getByPlaceholder('••••••••').fill(roles[role].password);
    await page.getByRole('button', {name: 'Přihlásit se'}).click();
    await waitForApp(page);
    await page.waitForFunction(() => Object.keys(sessionStorage).some((key) => key.startsWith('oidc.user:')));
    const oidc = await page.evaluate(() =>
        Object.entries(sessionStorage).filter(([key]) => key.startsWith('oidc.user:')));
    expect(oidc).not.toHaveLength(0);
    const session: RoleSession = {cookies: await page.context().cookies(), oidc};
    mkdirSync(dirname(sessionFile(role)), {recursive: true});
    writeFileSync(sessionFile(role), JSON.stringify(session));
    return session;
};

// Access tokens live only a few minutes, shorter than a full E2E run, so API-driven Given steps re-login when needed.
export const renewSession = async (browser: Browser, baseURL: string | undefined, role: Role) => {
    const context = await browser.newContext({baseURL, ignoreHTTPSErrors: true});
    try {
        return await loginAndStoreSession(await context.newPage(), role);
    } finally {
        await context.close();
    }
};
