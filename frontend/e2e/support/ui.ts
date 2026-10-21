import {expect, type Locator, type Page} from '@playwright/test';
import {toDisplayDate} from './dates';

const escapeRegExp = (text: string) => text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

// Form labels are rendered as <dt> next to the control, not as <label for>.
// Required fields append "*" to the accessible label.
const field = (page: Page, label: string) =>
    page.getByLabel(new RegExp(`^${escapeRegExp(label)}\\*?$`))
        .or(page.locator('dt', {hasText: new RegExp(`^${escapeRegExp(label)}( \\*)?$`)}).locator('xpath=following-sibling::dd').locator('input, select, textarea'))
        .first();

// Action buttons may carry the aria-label "Select <label> form" (HalFormTemplateButton; a11y follow-up).
export const action = (scope: Page | Locator, label: string) => {
    const escaped = escapeRegExp(label);
    return scope.getByRole('button', {name: new RegExp(`^(${escaped}|Select ${escaped} form)$`)})
        .or(scope.getByRole('link', {name: label, exact: true}));
};

export const click = (page: Page, label: string) => action(page, label).first().click();

export const fill = (page: Page, label: string, value: string) => field(page, label).fill(value);

export const select = (page: Page, label: string, option: string) => field(page, label).selectOption({label: option});

// The multi-select combobox has no accessible name yet, so it is located through the wrapper holding the field label.
export const pick = async (page: Page, label: string, option: string) => {
    const wrapper = page.locator('label', {hasText: new RegExp(`^${escapeRegExp(label)}\\*?$`)}).locator('xpath=..');
    await wrapper.getByRole('combobox').fill(option);
    await wrapper.getByRole('option', {name: option, exact: true}).click();
    await expect(wrapper.getByRole('button', {name: `Odebrat ${option}`})).toBeVisible();
};

// Waits for the backend answer to the submitted form, so a following step neither aborts the request by navigating
// away nor checks the outcome too early. A refused form stays open with its error for the Then step to observe.
export const submitWith = async (page: Page, trigger: Locator) => {
    const answered = page.waitForResponse((response) => response.url().includes('/api/') && response.request().method() !== 'GET');
    await trigger.click();
    const response = await answered;
    if (response.ok()) {
        await expect(page.getByRole('dialog')).toHaveCount(0);
    }
    return response;
};

export const submit = (page: Page, label = 'Odeslat') => submitWith(page, action(page, label).first());

export const expectRefused = (page: Page) => expect(page.getByRole('alert').first()).toBeVisible();

// Deep links reload the app through the OAuth2 redirect and lose the path, so the SPA router is driven directly.
export const navigateInApp = (page: Page, path: string) =>
    page.evaluate((target) => {
        window.history.pushState({}, '', target);
        window.dispatchEvent(new PopStateEvent('popstate'));
    }, path);

// The sidebar comes first; breadcrumbs are navigation landmarks too.
export const menuEntry = (page: Page, item: string) =>
    page.getByRole('navigation').getByRole('link', {name: item, exact: true}).first();

// A refused form stays open in its modal, which would block the menu.
export const openFromMenu = async (page: Page, item: string) => {
    const closeDialog = page.getByRole('dialog').getByRole('button', {name: 'Close modal'});
    if (await closeDialog.count()) {
        await closeDialog.click();
    }
    await menuEntry(page, item).click();
};

export const heading = (page: Page, name: string) => page.getByRole('heading', {name, exact: true});

export const section = (page: Page, name: string) => heading(page, name).locator('xpath=..');

export const displayedDate = (scope: Page | Locator, iso: string) => scope.getByText(toDisplayDate(iso), {exact: true});

// Matches an amount rendered by Intl.NumberFormat('cs-CZ'), which groups thousands with a (narrow) no-break space
// and writes a negative amount with a minus sign (hyphen-minus or U+2212); a positive pattern also matches the
// absolute value of a negative amount, which transaction history rows (a charge shows as a negative amount) rely on.
export const czechAmount = (amount: number) => {
    const grouped = String(Math.abs(amount)).replace(/\B(?=(\d{3})+(?!\d))/g, '[\\s\\u00a0\\u202f]?');
    return amount < 0
        ? new RegExp(`[-\\u2212]\\s?${grouped},00`)
        : new RegExp(`(^|[^\\d\\s\\u00a0\\u202f])${grouped},00`);
};

// Closes every open modal (warnings stay open over the page they belong to).
export const dismissDialogs = async (page: Page) => {
    const dialog = page.getByRole('dialog');
    for (let open = await dialog.count(); open > 0; open = await dialog.count()) {
        await dialog.last().getByRole('button', {name: /^(Close modal|Zavřít)$/}).first().click();
        await expect.poll(() => dialog.count()).toBeLessThan(open);
    }
};
