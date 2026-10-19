import {writeFileSync} from 'node:fs';
import {expect, test as setup} from '@playwright/test';
import {authFile, oidcSessionFile, type Role, roles} from './support/roles';

for (const role of Object.keys(roles) as Role[]) {
    setup(`authenticate as ${role}`, async ({page}) => {
        await page.goto('/');
        await page.getByPlaceholder('např. 12345').fill(roles[role].user);
        await page.getByPlaceholder('••••••••').fill(roles[role].password);
        await page.getByRole('button', {name: 'Přihlásit se'}).click();
        await expect(page.getByRole('navigation').first()).toBeAttached();
        await page.waitForLoadState('networkidle');
        const oidcEntries = await page.evaluate(() =>
            Object.entries(sessionStorage).filter(([key]) => key.startsWith('oidc.user:')));
        expect(oidcEntries).not.toHaveLength(0);
        writeFileSync(oidcSessionFile(role), JSON.stringify(oidcEntries));
        await page.context().storageState({path: authFile(role)});
    });
}
