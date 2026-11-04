import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {waitForApp} from '../support/app';
import {eventAppPath} from '../support/eventsApi';
import {test} from '../support/fixtures';
import {CATALOG} from '../support/membershipFeesPages';
import {personas, type Persona} from '../support/personas';
import {roles} from '../support/roles';
import {heading} from '../support/ui';

const {Given, When, Then} = createBdd(test);

const registrationNumberInput = (page: Page) => page.getByPlaceholder('např. 12345');

Given('nobody is signed in', async ({page, currentRole}) => {
    await page.context().clearCookies();
    currentRole.value = undefined;
});

// The authorization server cookie stays, the application session held in the browser tab is dropped.
Given('{persona} signed in earlier but the application session has ended', async ({page, actAs, currentRole}, persona: Persona) => {
    await actAs(persona);
    await page.evaluate(() => sessionStorage.clear());
    currentRole.value = undefined;
});

When('{persona} opens the bookmarked link to event {name}', async ({page, apiAs}, _persona: Persona, name: string) => {
    await page.goto(await eventAppPath(apiAs('admin'), name));
});

When('the shared link to the membership fee tiers page is opened', async ({page}) => {
    await page.goto('/membership-fee-tiers');
});

When('{persona} signs in on the login page', async ({page}, persona: Persona) => {
    const {user, password} = roles[personas[persona]];
    await registrationNumberInput(page).fill(user);
    await page.getByPlaceholder('••••••••').fill(password);
    await page.getByRole('button', {name: 'Přihlásit se'}).click();
});

Then('the event detail of {name} is shown', async ({page}, name: string) => {
    await waitForApp(page);
    await expect(heading(page, name)).toBeVisible();
});

Then('the membership fee tiers page is shown', async ({page}) => {
    await expect(heading(page, CATALOG)).toBeVisible();
});

Then('the sign-in completed without the login page', async ({page}) => {
    await expect(heading(page, CATALOG)).toBeVisible();
    await expect(registrationNumberInput(page)).toHaveCount(0);
});
