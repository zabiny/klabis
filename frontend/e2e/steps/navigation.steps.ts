import {expect} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {waitForApp} from '../support/app';
import {test} from '../support/fixtures';

const {When, Then} = createBdd(test);

When('I open the application', async ({page}) => {
    await page.goto('/');
    await waitForApp(page);
});

Then(/^the main menu (shows|does not show) "(.*)"$/, async ({page}, visibility: string, item: string) => {
    const entry = page.getByRole('navigation').getByRole('link', {name: item, exact: true});
    if (visibility === 'shows') {
        await expect(entry).toBeVisible();
    } else {
        await expect(entry).toHaveCount(0);
    }
});
