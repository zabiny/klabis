import {expect} from '@playwright/test';
import {createBdd, type DataTable} from 'playwright-bdd';
import {test} from '../support/fixtures';
import type {Persona} from '../support/personas';
import {menuEntry} from '../support/ui';

const {When, Then} = createBdd(test);

When('{persona} opens the application', async ({actAs}, persona: Persona) => {
    await actAs(persona);
});

When('{persona} navigates back', async ({page, actAs}, persona: Persona) => {
    await actAs(persona);
    await page.goBack();
});

Then('the main menu offers:', async ({page}, items: DataTable) => {
    for (const [item] of items.raw()) {
        await expect(menuEntry(page, item)).toBeVisible();
    }
});

Then('the main menu does not offer:', async ({page}, items: DataTable) => {
    for (const [item] of items.raw()) {
        await expect(menuEntry(page, item)).toHaveCount(0);
    }
});
