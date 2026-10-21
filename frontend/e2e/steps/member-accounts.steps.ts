import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';
import {depositTo, findMemberPath} from '../support/membersApi';
import {expectActing, type Persona} from '../support/personas';
import {action, click, czechAmount, heading, openFromMenu} from '../support/ui';
import {memberRow, openMemberDetail, searchMembers} from './members.steps';

const {Given, When, Then} = createBdd(test);

const balanceCard = (page: Page) => page.getByText('Zůstatek', {exact: true}).locator('xpath=..');

const openMemberAccount = async (page: Page, lastName: string) => {
    await openMemberDetail(page, lastName);
    await click(page, 'Účet');
    await expect(heading(page, 'Historie transakcí')).toBeVisible();
};

const recordTransaction = async (page: Page, lastName: string, tab: string, amount: number) => {
    await searchMembers(page, lastName);
    await expect(memberRow(page, lastName)).toHaveCount(1);
    await memberRow(page, lastName).getByRole('button', {name: 'Provést finanční transakci'}).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByText(lastName)).toBeVisible();
    await dialog.getByRole('tab', {name: tab}).click();
    await dialog.getByLabel('Částka *').fill(String(amount));
    const answered = page.waitForResponse((response) => response.url().includes('/api/') && response.request().method() === 'POST');
    await dialog.getByRole('button', {name: tab}).last().click();
    expect((await answered).ok()).toBe(true);
    await expect(dialog).toHaveCount(0);
};

Given('the account of member {name} holds {int} CZK', async ({apiAs}, lastName: string, amount: number) => {
    const admin = apiAs('admin');
    await depositTo(admin, await findMemberPath(admin, lastName), amount);
});

When('{persona} opens Finance from the main menu', async ({page, actAs}, persona: Persona) => {
    await actAs(persona);
    await openFromMenu(page, 'Finance');
    await expect(heading(page, 'Historie transakcí')).toBeVisible();
});

When('{persona} records a deposit of {int} CZK for member {name}', async ({page, actAs}, persona: Persona, amount: number, lastName: string) => {
    await actAs(persona);
    await recordTransaction(page, lastName, 'Připsání vkladu', amount);
});

When('{persona} records a charge of {int} CZK for member {name}', async ({page, actAs}, persona: Persona, amount: number, lastName: string) => {
    await actAs(persona);
    await recordTransaction(page, lastName, 'Stržení částky', amount);
});

When('{persona} reverses the deposit of {int} CZK on the account of member {name}', async ({page, actAs}, persona: Persona, amount: number, lastName: string) => {
    await actAs(persona);
    await openMemberAccount(page, lastName);
    await page.getByRole('row').filter({hasText: 'Vklad'}).filter({hasText: czechAmount(amount)}).getByRole('button', {name: 'Stornovat'}).click();
    const answered = page.waitForResponse((response) => response.url().includes('/api/') && response.request().method() === 'POST');
    await action(page.getByRole('dialog'), 'Potvrdit storno').click();
    expect((await answered).ok()).toBe(true);
    await expect(page.getByRole('dialog')).toHaveCount(0);
});

Then('{persona} sees the balance and the transaction history', async ({page, currentRole}, persona: Persona) => {
    expectActing(currentRole, persona);
    await expect(balanceCard(page)).toBeVisible();
    await expect(heading(page, 'Historie transakcí')).toBeVisible();
});

Then('the account of member {name} shows the balance {int} CZK', async ({page}, lastName: string, amount: number) => {
    await openMemberAccount(page, lastName);
    await expect(balanceCard(page).getByText(czechAmount(amount))).toBeVisible();
});

Then('the history of member {name} lists the transaction of {int} CZK', async ({page}, _lastName: string, amount: number) => {
    await expect(page.getByRole('row').filter({hasText: czechAmount(amount)}).first()).toBeVisible();
});

Then('the history of member {name} marks the reversal of the deposit of {int} CZK', async ({page}, _lastName: string, amount: number) => {
    await expect(page.getByRole('row').filter({hasText: /Storno(?!vá)/}).filter({hasText: czechAmount(amount)})).toBeVisible();
    await expect(page.getByRole('row').filter({hasText: 'Stornováno'}).filter({hasText: czechAmount(amount)})).toBeVisible();
});
