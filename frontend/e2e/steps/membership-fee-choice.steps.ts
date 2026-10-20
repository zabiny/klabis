import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test, type ApiClient, type FeeScenarioState} from '../support/fixtures';
import {memberIdentity} from '../support/halApi';
import {expectActing, personas, type Persona} from '../support/personas';
import type {Role} from '../support/roles';
import {action, czechAmount, heading, navigateInApp, openFromMenu, submitWith} from '../support/ui';

const {When, Then} = createBdd(test);

const FEE_SECTION = 'ČLENSKÝ PŘÍSPĚVEK';
const LOCKED = 'Volba je uzamčena.';
const CHOOSE_PROMPT = 'Probíhá volba tieru příspěvku. Zvolte si svůj tier.';
const CHOICE_ACTION = /^(Zvolit tier|Změnit tier)$/;

const feeSection = (page: Page) => heading(page, FEE_SECTION).locator('xpath=..');

// The profile is linked from the home page only.
const openProfile = async (page: Page) => {
    await page.getByRole('link', {name: 'Klabis', exact: true}).click();
    await page.getByText('Můj profil', {exact: true}).click();
    await expect(feeSection(page).getByText('Načítání...')).toHaveCount(0);
    await expect(feeSection(page).locator('p').first()).toBeVisible();
};

// The form is not linked from the profile once voting is closed, so it is reached through the SPA router.
const openChoiceForm = async (page: Page, apiAs: (role: Role) => ApiClient, persona: Persona, feeState: FeeScenarioState) => {
    const {id} = await memberIdentity(apiAs(personas[persona]));
    await navigateInApp(page, `/members/${id}/fee-choice/${feeState.year}`);
    await expect(page.getByRole('heading', {name: new RegExp(`pro rok ${feeState.year}$`), level: 1})).toBeVisible();
};

const yearlyFeeRow = (page: Page, year: number) =>
    page.getByRole('row').filter({hasText: `Roční členský příspěvek ${year}`});

// The transactions table shows its empty message while still loading, so the page counts as open once the list arrives.
const openAccount = async (page: Page) => {
    const transactions = page.waitForResponse(response =>
        response.request().method() === 'GET' && /\/account\/transactions\?/.test(response.url()));
    await openFromMenu(page, 'Finance');
    expect((await transactions).ok()).toBe(true);
    await expect(page.getByRole('table').locator('tbody tr').first()).toBeVisible();
};

// Choosing and changing the level is the same action on the profile; the scenarios name it by its business meaning.
for (const phrase of ['chooses level', 'changes the fee level choice to']) {
    When(`{persona} ${phrase} {name}`, async ({page, actAs}, persona: Persona, name: string) => {
        await actAs(persona);
        await openProfile(page);
        await feeSection(page).getByRole('link', {name: CHOICE_ACTION}).click();
        await page.getByRole('radio', {name}).click();
        expect((await submitWith(page, page.getByRole('button', {name: CHOICE_ACTION}))).ok()).toBe(true);
    });
}

When('{persona} withdraws the fee level choice', async ({page, actAs, apiAs, feeState}, persona: Persona) => {
    await actAs(persona);
    await openChoiceForm(page, apiAs, persona, feeState);
    expect((await submitWith(page, action(page, 'Zrušit volbu'))).ok()).toBe(true);
});

When('{persona} views the profile', async ({page, actAs}, persona: Persona) => {
    await actAs(persona);
    await openProfile(page);
});

When('{persona} opens the fee level choice form', async ({page, actAs, apiAs, feeState}, persona: Persona) => {
    await actAs(persona);
    await openChoiceForm(page, apiAs, persona, feeState);
});

Then('{persona} can choose level {name}', async ({page, actAs, apiAs, feeState}, persona: Persona, name: string) => {
    await actAs(persona);
    await openChoiceForm(page, apiAs, persona, feeState);
    await expect(page.getByRole('radio', {name})).toBeVisible();
});

Then('{persona}\'s profile shows fee level {name}', async ({page, actAs}, persona: Persona, name: string) => {
    await actAs(persona);
    await openProfile(page);
    await expect(feeSection(page).getByText(name, {exact: true})).toBeVisible();
});

Then('{persona}\'s profile shows fee level {name} as locked', async ({page, actAs}, persona: Persona, name: string) => {
    await actAs(persona);
    await openProfile(page);
    await expect(feeSection(page).getByText(name, {exact: true})).toBeVisible();
    await expect(feeSection(page).getByText(LOCKED)).toBeVisible();
});

Then('{persona}\'s profile prompts to choose a fee level', async ({page, actAs}, persona: Persona) => {
    await actAs(persona);
    await openProfile(page);
    await expect(feeSection(page).getByText(CHOOSE_PROMPT)).toBeVisible();
});

Then('{persona} is not offered to change the fee level choice', async ({page, actAs}, persona: Persona) => {
    await actAs(persona);
    await openProfile(page);
    await expect(feeSection(page).getByRole('link', {name: CHOICE_ACTION})).toHaveCount(0);
});

Then('level {name} is pre-selected', async ({page}, name: string) => {
    await expect(page.getByRole('radio', {name})).toHaveAttribute('aria-checked', 'true');
});

Then('the choice form shows level {name} as locked', async ({page}, name: string) => {
    await expect(page.getByText(LOCKED)).toBeVisible();
    await expect(page.getByText(name, {exact: true})).toBeVisible();
});

Then('{persona} is not offered to withdraw the fee level choice', async ({page, currentRole}, persona: Persona) => {
    expectActing(currentRole, persona);
    await expect(page.getByRole('heading', {name: /^Výběr tieru členského příspěvku/, level: 1})).toBeVisible();
    await expect(action(page, 'Zrušit volbu')).toHaveCount(0);
});

Then('{persona}\'s account shows the yearly membership fee of {int} CZK for the campaign year',
    async ({page, actAs, feeState}, persona: Persona, amount: number) => {
        await actAs(persona);
        await openAccount(page);
        await expect(yearlyFeeRow(page, feeState.year!).getByText(czechAmount(amount))).toBeVisible();
    });

Then('{persona}\'s account shows no yearly membership fee for the campaign year', async ({page, actAs, feeState}, persona: Persona) => {
    await actAs(persona);
    await openAccount(page);
    await expect(yearlyFeeRow(page, feeState.year!)).toHaveCount(0);
});
