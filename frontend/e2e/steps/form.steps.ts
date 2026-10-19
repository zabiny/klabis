import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';

const {When, Then} = createBdd(test);

// Form labels are rendered as <dt> next to the control, not as <label for>.
const field = (page: Page, label: string) =>
    page.getByLabel(label, {exact: true})
        .or(page.locator('dt', {hasText: new RegExp(`^${label}$`)}).locator('xpath=following-sibling::dd').locator('input, select, textarea'))
        .first();

When('I click {string}', async ({page, resolve}, label: string) => {
    const name = resolve(label);
    const exact = page.getByRole('button', {name, exact: true}).or(page.getByRole('link', {name, exact: true}));
    // Some action buttons carry a longer aria-label (e.g. "Select <label> form").
    const partial = page.getByRole('button', {name});
    await (await exact.count() > 0 ? exact : partial).first().click();
});

When('I fill {string} with {string}', async ({page, resolve}, label: string, value: string) => {
    await field(page, label).fill(resolve(value));
});

When('I select {string} in {string}', async ({page}, option: string, label: string) => {
    await field(page, label).selectOption({label: option});
});

Then('I see {string}', async ({page, resolve}, text: string) => {
    await expect(page.getByText(resolve(text), {exact: false}).first()).toBeVisible();
});
