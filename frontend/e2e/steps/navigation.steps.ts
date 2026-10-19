import {expect} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {waitForApp} from '../support/app';
import {test} from '../support/fixtures';

const {When, Then} = createBdd(test);

When('I open the application', async ({page}) => {
    await page.goto('/');
    await waitForApp(page);
});

When('I open my profile', async ({page}) => {
    await page.getByText('Můj profil', {exact: true}).click();
});

When('I navigate back', async ({page}) => {
    await page.goBack();
});

// Deep links reload the app through the OAuth2 redirect and lose the path, so the SPA router is driven directly.
When('I navigate in the application to {text}', async ({page}, path: string) => {
    await page.evaluate((target) => {
        window.history.pushState({}, '', target);
        window.dispatchEvent(new PopStateEvent('popstate'));
    }, path);
});

Then(/^the main menu (shows|does not show) "(.*)"$/, async ({page}, visibility: string, item: string) => {
    const entry = page.getByRole('navigation').getByRole('link', {name: item, exact: true});
    if (visibility === 'shows') {
        await expect(entry).toBeVisible();
    } else {
        await expect(entry).toHaveCount(0);
    }
});
