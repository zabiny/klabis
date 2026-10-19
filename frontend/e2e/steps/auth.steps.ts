import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';
import type {Role} from '../support/roles';

const {Given, When} = createBdd(test);

Given('I am logged in as {string}', async ({loginAs}, role: string) => {
    await loginAs(role as Role);
});

// The OIDC user lives in sessionStorage of the current tab, so it has to be dropped together with the cookies.
When('I continue as {string}', async ({page, loginAs}, role: string) => {
    await page.context().clearCookies();
    await page.evaluate(() => sessionStorage.clear());
    await loginAs(role as Role);
});
