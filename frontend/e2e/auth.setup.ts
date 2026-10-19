import {mkdirSync, writeFileSync} from 'node:fs';
import {dirname} from 'node:path';
import {expect, test as setup} from '@playwright/test';
import {waitForApp} from './support/app';
import {type Role, roles, type RoleSession, sessionFile} from './support/roles';

for (const role of Object.keys(roles) as Role[]) {
    setup(`authenticate as ${role}`, async ({page}) => {
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
    });
}
