import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';
import {expectActing, type Persona} from '../support/personas';
import {openFeeHistoryFromDetail} from '../support/membershipFeesPages';
import {openOwnProfile} from './members.steps';
import {openFeeHistoryOfMember} from './membership-fee-history.steps';
import {czechAmount, heading, openFromMenu, submitWith} from '../support/ui';

const {When, Then} = createBdd(test);

const FEE_SECTION = 'ČLENSKÝ PŘÍSPĚVEK';
const CHOOSE_PROMPT = 'Probíhá volba typu členského příspěvku. Zvolte si svůj typ.';
const CURRENT_TYPE = 'Aktuální typ členského příspěvku';
const CHOICE_ACTION = /^(Zvolit|Změnit) typ členského příspěvku$/;

const feeSection = (page: Page) => heading(page, FEE_SECTION).locator('xpath=..');

const lockedType = (page: Page) =>
    feeSection(page).locator('div').filter({has: page.getByText(CURRENT_TYPE, {exact: true})}).last();

// An open choice shows the chosen type pre-selected in the form; once locked it is shown as the current type.
const currentType = (page: Page, name: string) =>
    lockedType(page).getByText(name, {exact: true})
        .or(feeSection(page).getByRole('radio', {name: new RegExp(`^${name}\\b`), checked: true}));

// The member's own fee page is reached through the profile, which is linked from the home page only.
const openProfile = async (page: Page) => {
    await openOwnProfile(page);
    await openFeeHistoryFromDetail(page);
};

const submitChoice = async (page: Page, name: string) => {
    await feeSection(page).getByRole('radio', {name}).click();
    expect((await submitWith(page, feeSection(page).getByRole('button', {name: CHOICE_ACTION}))).ok()).toBe(true);
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

// Choosing and changing the type is the same action in the section; the scenarios name it by its business meaning.
for (const phrase of ['chooses type', 'changes the fee type choice to']) {
    When(`{persona} ${phrase} {name}`, async ({page, actAs}, persona: Persona, name: string) => {
        await actAs(persona);
        await openProfile(page);
        await submitChoice(page, name);
    });
}

When('{persona} chooses type {name} for member {string}',
    async ({page, actAs, minorChoices}, persona: Persona, name: string, fullName: string) => {
        await openFeeHistoryOfMember(page, actAs, persona, fullName);
        minorChoices.withdrawAfterScenario(persona, fullName);
        await submitChoice(page, name);
    });

When('{persona} withdraws the fee type choice', async ({page, actAs}, persona: Persona) => {
    await actAs(persona);
    await openProfile(page);
    expect((await submitWith(page, feeSection(page).getByRole('button', {name: 'Zrušit volbu'}))).ok()).toBe(true);
});

When('{persona} opens the fee page', async ({page, actAs}, persona: Persona) => {
    await actAs(persona);
    await openProfile(page);
});

Then('{persona} can choose type {name}', async ({page, actAs}, persona: Persona, name: string) => {
    await actAs(persona);
    await openProfile(page);
    await expect(feeSection(page).getByRole('radio', {name})).toBeVisible();
});

Then('{persona}\'s fee section offers type {name} for {int} CZK', async ({page, currentRole}, persona: Persona, name: string, amount: number) => {
    expectActing(currentRole, persona);
    await expect(feeSection(page).getByRole('radio', {name: new RegExp(`${name}\\s*${amount} Kč`)})).toBeVisible();
});

Then('{persona}\'s fee section shows fee type {name}', async ({page, currentRole}, persona: Persona, name: string) => {
    expectActing(currentRole, persona);
    await expect(currentType(page, name)).toBeVisible();
});

Then('{persona}\'s fee section prompts to choose a fee type', async ({page, currentRole}, persona: Persona) => {
    expectActing(currentRole, persona);
    await expect(feeSection(page).getByText(CHOOSE_PROMPT)).toBeVisible();
    await expect(feeSection(page).getByText(CURRENT_TYPE)).toHaveCount(0);
});

Then('{persona} does not see the fee section', async ({page, currentRole}, persona: Persona) => {
    expectActing(currentRole, persona);
    await expect(heading(page, 'Historie příspěvků')).toBeVisible();
    await expect(heading(page, FEE_SECTION)).toHaveCount(0);
});

Then('type {name} is pre-selected', async ({page}, name: string) => {
    await expect(feeSection(page).getByRole('radio', {name})).toHaveAttribute('aria-checked', 'true');
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

Then('{persona} sees fee type {name} in the fee section of member {string}',
    async ({page, actAs}, persona: Persona, name: string, fullName: string) => {
        await openFeeHistoryOfMember(page, actAs, persona, fullName);
        await expect(currentType(page, name)).toBeVisible();
    });
