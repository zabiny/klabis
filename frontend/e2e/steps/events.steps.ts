import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {localIsoDate} from '../support/dates';
import {createDraftEvent, createOpenEvent, registerForEvent} from '../support/eventsApi';
import {test} from '../support/fixtures';
import {memberIdentity} from '../support/halApi';
import {assignLevel, firstPublishedLevelName, withdrawChoice} from '../support/membershipFeesApi';
import {personas, type Persona} from '../support/personas';
import {action, click, fill, heading, openFromMenu, select, submit} from '../support/ui';

const {Given, When, Then} = createBdd(test);

const openEventList = async (page: Page, name: string) => {
    await openFromMenu(page, 'Akce');
    await fill(page, 'Hledat', name);
};

const openEvent = async (page: Page, name: string) => {
    await openEventList(page, name);
    await page.getByRole('cell', {name}).first().click();
    await expect(heading(page, name)).toBeVisible();
};

const registrations = (page: Page) => page.getByRole('table').filter({hasText: 'Příjmení'});

const registrationRow = (page: Page, firstName: string, lastName: string) =>
    registrations(page).getByRole('row').filter({hasText: lastName}).filter({hasText: firstName});

Given('a draft event {name} exists', async ({apiAs}, name: string) => {
    await createDraftEvent(apiAs('admin'), name);
});

Given('event {name} is open for registration', async ({apiAs}, name: string) => {
    await createOpenEvent(apiAs('admin'), name);
});

// Earlier campaign scenarios leave the member sanctioned, which blocks new registrations. An emergency assignment in the
// active campaign lifts the block; the choice is withdrawn again so the scenario continues without a level.
Given('{persona} is registered for the open event {name}', async ({apiAs, feeState}, persona: Persona, name: string) => {
    const admin = apiAs('admin');
    const member = apiAs(personas[persona]);
    await createOpenEvent(admin, name);
    await assignLevel(admin, await memberIdentity(member), feeState.year!, await firstPublishedLevelName(admin, feeState.year!));
    await registerForEvent(member, name);
    await withdrawChoice(member, feeState.year!);
});

When('{persona} creates event {name} filling in only the required fields', async ({page, actAs}, persona: Persona, name: string) => {
    await actAs(persona);
    await openFromMenu(page, 'Akce');
    await click(page, 'Přidat akci');
    await fill(page, 'Název', name);
    await fill(page, 'Datum konání', localIsoDate(60));
    await fill(page, 'Pořadatel', 'ZBM');
    await select(page, 'Použití SI čipu', 'Nepoužívá se');
    await submit(page);
    await expect(heading(page, name)).toBeVisible();
});

When('{persona} changes the location of event {name} to {string}', async ({page, actAs}, persona: Persona, name: string, location: string) => {
    await actAs(persona);
    await openEvent(page, name);
    await click(page, 'Upravit');
    await fill(page, 'Místo', location);
    await submit(page, 'Uložit změny');
});

Then('event {name} is a draft', async ({page}, name: string) => {
    await openEvent(page, name);
    await expect(page.getByText('Koncept', {exact: true}).first()).toBeVisible();
});

Then('the event list contains {name}', async ({page}, name: string) => {
    await openEventList(page, name);
    await expect(page.getByRole('cell', {name}).first()).toBeVisible();
});

Then('event {name} takes place in {string}', async ({page}, name: string, location: string) => {
    await openEvent(page, name);
    await expect(page.getByText(location, {exact: true}).first()).toBeVisible();
});

Then('{persona} remains registered for event {name}', async ({page, actAs, apiAs}, persona: Persona, name: string) => {
    const {firstName, lastName} = await memberIdentity(apiAs(personas[persona]));
    await actAs(persona);
    await openEvent(page, name);
    await expect(registrationRow(page, firstName, lastName)).toBeVisible();
});

Then('{persona}\'s registration for event {name} is cancelled', async ({page, actAs, apiAs}, persona: Persona, name: string) => {
    const {firstName, lastName} = await memberIdentity(apiAs(personas[persona]));
    await actAs(persona);
    await openEvent(page, name);
    await expect(registrations(page).locator('tbody tr').first()).toBeVisible();
    await expect(registrationRow(page, firstName, lastName)).toHaveCount(0);
});

Then('{persona} is offered to register for event {name}', async ({page, actAs}, persona: Persona, name: string) => {
    await actAs(persona);
    await openEvent(page, name);
    await expect(action(page, 'Přihlásit se').first()).toBeVisible();
});

Then('{persona} is not offered to register for event {name}', async ({page, actAs}, persona: Persona, name: string) => {
    await actAs(persona);
    await openEvent(page, name);
    await expect(registrations(page).locator('tbody tr').first()).toBeVisible();
    await expect(action(page, 'Přihlásit se')).toHaveCount(0);
});
