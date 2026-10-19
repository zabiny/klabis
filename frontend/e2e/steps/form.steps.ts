import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';

const {When, Then} = createBdd(test);

const escapeRegExp = (text: string) => text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

// Form labels are rendered as <dt> next to the control, not as <label for>.
// Required fields append "*" to the accessible label.
const field = (page: Page, label: string) =>
    page.getByLabel(new RegExp(`^${escapeRegExp(label)}\\*?$`))
        .or(page.locator('dt', {hasText: new RegExp(`^${escapeRegExp(label)}$`)}).locator('xpath=following-sibling::dd').locator('input, select, textarea'))
        .first();

When('I click {text}', async ({page}, label: string) => {
    const escaped = escapeRegExp(label);
    // Action buttons may carry the aria-label "Select <label> form" (HalFormTemplateButton; a11y follow-up).
    const button = page.getByRole('button', {name: new RegExp(`^(${escaped}|Select ${escaped} form)$`)});
    await button.or(page.getByRole('link', {name: label, exact: true})).first().click();
});

When('I fill {text} with {text}', async ({page}, label: string, value: string) => {
    await field(page, label).fill(value);
});

When('I select {text} in {text}', async ({page}, option: string, label: string) => {
    await field(page, label).selectOption({label: option});
});

Then('I see {text}', async ({page}, text: string) => {
    await expect(page.getByText(text).first()).toBeVisible();
});

Then('I do not see {text}', async ({page}, text: string) => {
    await expect(page.getByText(text)).toHaveCount(0);
});

Then('I see an error message', async ({page}) => {
    await expect(page.getByRole('alert').first()).toBeVisible();
});

When('I tick {text} in {text}', async ({page}, option: string, label: string) => {
    const container = page.locator('dt', {hasText: new RegExp(`^${escapeRegExp(label)}$`)}).locator('xpath=following-sibling::dd');
    await container.getByRole('checkbox', {name: option}).or(container.getByLabel(option)).first().check();
});

When('I open the list row {text}', async ({page}, text: string) => {
    await page.getByRole('cell', {name: text}).first().click();
});
