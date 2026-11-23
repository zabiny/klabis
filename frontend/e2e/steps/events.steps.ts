import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {localIsoDate} from '../support/dates';
import {createDraftEvent, createOpenEvent, registerForEvent, registerMemberForEvent} from '../support/eventsApi';
import {test} from '../support/fixtures';
import {memberIdentity} from '../support/halApi';
import {findMemberHrefByName, findMemberIdByName, splitName} from '../support/membersApi';
import {assignLevel, firstPublishedLevelName, withdrawChoice} from '../support/membershipFeesApi';
import {personas, type Persona} from '../support/personas';
import {openEvent, openEventList} from '../support/eventsPages';
import {action, click, fill, heading, openFromMenu, select, submit} from '../support/ui';

const {Given, When, Then} = createBdd(test);

const registrations = (page: Page) => page.getByRole('table').filter({hasText: 'Příjmení'});

const registrationRow = (page: Page, firstName: string, lastName: string) =>
    registrations(page).getByRole('row').filter({hasText: lastName}).filter({hasText: firstName});

type RegistrationOwner = {firstName: string; lastName: string};

const openRegistration = async (page: Page, name: string, owner: RegistrationOwner) => {
    await openEvent(page, name);
    const row = registrationRow(page, owner.firstName, owner.lastName);
    await expect(row, `the registration of ${owner.firstName} ${owner.lastName} is listed`).toBeVisible();
    await action(row, 'Upravit přihlášku').first().click();
};

const expectRegisteredIn = async (page: Page, name: string, owner: RegistrationOwner, category: string) => {
    await openEvent(page, name);
    await expect(registrationRow(page, owner.firstName, owner.lastName).filter({hasText: category})).toBeVisible();
};

const expectRegistrationCancelled = async (page: Page, name: string, owner: RegistrationOwner) => {
    await openEvent(page, name);
    await expect(registrations(page).locator('tbody tr').first()).toBeVisible();
    await expect(registrationRow(page, owner.firstName, owner.lastName)).toHaveCount(0);
};

Given('a draft event {name} exists', async ({apiAs}, name: string) => {
    await createDraftEvent(apiAs('admin'), name);
});

Given('event {name} is open for registration', async ({apiAs}, name: string) => {
    await createOpenEvent(apiAs('admin'), name);
});

Given('event {name} is open for registration in categories {string}', async ({apiAs}, name: string, categories: string) => {
    await createOpenEvent(apiAs('admin'), name, {categories: categories.split(',').map((category) => category.trim())});
});

// Registration is blocked for members who missed the fee selection of a previous campaign. An assignment in the active
// campaign lifts the block; the guardian withdraws the choice again so the shared example-data minors stay untouched.
Given('minor {string} is registered for event {name} in category {string}', async ({apiAs, feeState}, minor: string, name: string, category: string) => {
    const admin = apiAs('admin');
    const {firstName, lastName} = splitName(minor);
    const id = await findMemberIdByName(admin, firstName, lastName);
    await assignLevel(admin, {id}, feeState, await firstPublishedLevelName(admin, feeState));
    await registerMemberForEvent(admin, name, id, category);
    await withdrawChoice(apiAs('guardian'), await findMemberHrefByName(admin, firstName, lastName));
});

// Earlier campaign scenarios leave the member sanctioned, which blocks new registrations. An emergency assignment in the
// active campaign lifts the block; the choice is withdrawn again so the scenario continues without a type.
Given('{persona} is registered for the open event {name}', async ({apiAs, feeState}, persona: Persona, name: string) => {
    const admin = apiAs('admin');
    const member = apiAs(personas[persona]);
    await createOpenEvent(admin, name);
    await assignLevel(admin, await memberIdentity(member), feeState, await firstPublishedLevelName(admin, feeState));
    await registerForEvent(member, name);
    await withdrawChoice(member);
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

When('{persona} publishes event {name}', async ({page, actAs}, persona: Persona, name: string) => {
    await actAs(persona);
    await openEvent(page, name);
    await click(page, 'Publikovat');
    await submit(page);
});

When('{persona} cancels event {name} because of {string}', async ({page, actAs}, persona: Persona, name: string, reason: string) => {
    await actAs(persona);
    await openEvent(page, name);
    await click(page, 'Zrušit akci');
    await fill(page, 'Důvod zrušení (volitelné)', reason);
    await submit(page);
});

When('{persona} registers for event {name} in category {string}', async ({page, actAs}, persona: Persona, name: string, category: string) => {
    await actAs(persona);
    await openEvent(page, name);
    await click(page, 'Přihlásit se');
    await select(page, 'Kategorie', category);
    await submit(page, 'Potvrdit přihlášku');
});

const confirmUnregistering = async (page: Page) => {
    await action(page.getByRole('dialog'), 'Odhlásit').click();
    await submit(page);
};

When('{persona} unregisters from event {name}', async ({page, actAs, apiAs}, persona: Persona, name: string) => {
    await actAs(persona);
    await openRegistration(page, name, await memberIdentity(apiAs(personas[persona])));
    await confirmUnregistering(page);
});

When('{persona} unregisters {persona} from event {name}', async ({page, actAs, apiAs}, manager: Persona, member: Persona, name: string) => {
    const owner = await memberIdentity(apiAs(personas[member]));
    await actAs(manager);
    await openRegistration(page, name, owner);
    await confirmUnregistering(page);
});

Then('{persona} sees event {name} in the event list', async ({page, actAs}, persona: Persona, name: string) => {
    await actAs(persona);
    await openEventList(page, name);
    await expect(page.getByRole('cell', {name}).first()).toBeVisible();
});

Then('event {name} is cancelled because of {string}', async ({page}, name: string, reason: string) => {
    await openEvent(page, name);
    await expect(page.getByText(reason, {exact: true})).toBeVisible();
    await expect(page.getByText('Zrušeno', {exact: true}).first()).toBeVisible();
});

Then('{persona} is registered for event {name} in category {string}', async ({page, actAs, apiAs}, persona: Persona, name: string, category: string) => {
    const owner = await memberIdentity(apiAs(personas[persona]));
    await actAs(persona);
    await expectRegisteredIn(page, name, owner, category);
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
    const owner = await memberIdentity(apiAs(personas[persona]));
    await actAs(persona);
    await expectRegistrationCancelled(page, name, owner);
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

When('{persona} changes the category of minor {string} in event {name} to {string}', async ({page, actAs}, persona: Persona, minor: string, name: string, category: string) => {
    await actAs(persona);
    await openRegistration(page, name, splitName(minor));
    await select(page, 'Kategorie', category);
    await submit(page, 'Uložit změny');
});

When('{persona} unregisters minor {string} from event {name}', async ({page, actAs}, persona: Persona, minor: string, name: string) => {
    await actAs(persona);
    await openRegistration(page, name, splitName(minor));
    await confirmUnregistering(page);
});

Then('{persona} sees minor {string} registered for event {name} in category {string}', async ({page, actAs}, persona: Persona, minor: string, name: string, category: string) => {
    await actAs(persona);
    await expectRegisteredIn(page, name, splitName(minor), category);
});

Then('the registration of minor {string} for event {name} is cancelled', async ({page}, minor: string, name: string) => {
    await expectRegistrationCancelled(page, name, splitName(minor));
});
