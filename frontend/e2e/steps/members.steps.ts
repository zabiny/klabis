import {expect, type Locator, type Page} from '@playwright/test';
import {createBdd, type DataTable} from 'playwright-bdd';
import {scenarioPrefix} from '../support/parameters';
import {test} from '../support/fixtures';
import {type Linked} from '../support/halApi';
import {minorDateOfBirth} from '../support/dates';
import {createTrainingGroup, findMemberPath, guardianEmail, registerMember, REGISTERED_FIRST_NAME, registrationBody} from '../support/membersApi';
import {expectActing, personas, type Persona} from '../support/personas';
import {action, chooseCountry, click, dismissDialogs, fill, heading, openFromMenu, section, select, submit} from '../support/ui';

const {Given, When, Then} = createBdd(test);

// Returning to an already open member list keeps the typed text but not its filtering, so the list is always entered from the home page.
export const searchMembers = async (page: Page, text: string, status?: string) => {
    await dismissDialogs(page);
    await page.getByRole('link', {name: 'Klabis', exact: true}).click();
    await openFromMenu(page, 'Členové');
    if (status) {
        const pill = page.getByRole('button', {name: status, exact: true});
        await pill.click();
        await expect(pill).toHaveAttribute('aria-pressed', 'true');
    }
    const searchBox = page.getByRole('textbox', {name: 'Hledat člena'});
    await searchBox.fill('');
    await searchBox.fill(text);
};

// Table rows are exposed as buttons named "Row <n>" (a11y follow-up).
const namedMemberRow = (page: Page, firstName: string, lastName: string) =>
    page.getByRole('button', {name: /^Row \d+$/})
        .filter({has: page.getByRole('cell', {name: lastName, exact: true})})
        .filter({has: page.getByRole('cell', {name: firstName, exact: true})});

export const memberRow = (page: Page, lastName: string) => namedMemberRow(page, REGISTERED_FIRST_NAME, lastName);

export const openMemberDetailOf = async (page: Page, firstName: string, lastName: string, status?: string) => {
    await searchMembers(page, lastName, status);
    await namedMemberRow(page, firstName, lastName).getByRole('cell', {name: lastName}).first().click();
    await expect(heading(page, 'ADRESA')).toBeVisible();
};

export const openDetailAs = async (
    page: Page, actAs: (persona: Persona) => Promise<void>, persona: Persona, lastName: string, firstName = REGISTERED_FIRST_NAME,
) => {
    await actAs(persona);
    await openMemberDetailOf(page, firstName, lastName, personas[persona] === 'admin' ? 'Vše' : undefined);
};

export const openMemberDetail = (page: Page, lastName: string, status?: string) =>
    openMemberDetailOf(page, REGISTERED_FIRST_NAME, lastName, status);

export const openOwnProfile = async (page: Page) => {
    await page.getByRole('link', {name: 'Klabis', exact: true}).click();
    await page.getByText('Můj profil', {exact: true}).click();
    await expect(action(page, 'Upravit profil').first()).toBeVisible();
};

Given('member {name} is registered', async ({apiAs}, lastName: string) => {
    await registerMember(apiAs('admin'), lastName);
});

Given('the phone number of {persona} is {string}', async ({apiAs}, persona: Persona, phone: string) => {
    const member = apiAs(personas[persona]);
    const profile = await member.get((await member.root())._links!.profile.href) as Linked;
    await member.patch(member.templateTarget(profile, 'updateMember'), {phone});
});

const addGuardianRow = async (page: Page) => {
    await click(page, 'Přidat');
    return page.getByTestId('collection-item').last();
};

const chooseExistingGuardian = async (page: Page, lastName: string) => {
    const row = await addGuardianRow(page);
    await row.getByRole('textbox', {name: 'Hledat podle jména'}).fill(lastName);
    const label = await row.getByRole('option', {name: new RegExp(lastName)}).textContent();
    await row.getByRole('combobox').selectOption({label: label!});
};

const addNewGuardian = async (page: Page, lastName: string) => {
    const row = await addGuardianRow(page);
    await row.getByRole('button', {name: 'Založit nového zástupce'}).click();
    await row.getByRole('textbox', {name: 'Jméno', exact: true}).fill(REGISTERED_FIRST_NAME);
    await row.getByRole('textbox', {name: 'Příjmení', exact: true}).fill(lastName);
    await row.getByRole('textbox', {name: 'E-mail', exact: true}).fill(guardianEmail(lastName));
    await row.getByRole('textbox', {name: 'Telefon', exact: true}).fill('+420 777 333 444');
};

export const fillRegistrationDetails = async (page: Page, body: ReturnType<typeof registrationBody>, contact: boolean) => {
    await fill(page, 'Datum narození', body.dateOfBirth);
    await select(page, 'Pohlaví', 'Muž');
    await chooseCountry(page, 'Státní příslušnost', body.nationality);
    await fill(page, 'Rodné číslo', body.birthNumber);
    if (contact) {
        await fill(page, 'E-mail', body.email);
        await fill(page, 'Telefon', body.phone);
    }
    await fill(page, 'Ulice', body.address.street);
    await fill(page, 'Město', body.address.city);
    await fill(page, 'PSČ', body.address.postalCode);
    await chooseCountry(page, 'Stát', body.address.country);
};

const fillRegistrationForm = async (page: Page, lastName: string, body: ReturnType<typeof registrationBody>, contact = true) => {
    await click(page, 'Registrovat člena');
    await fill(page, 'Jméno', body.firstName);
    await fill(page, 'Příjmení', lastName);
    await fillRegistrationDetails(page, body, contact);
};

Given('member {name} is the only trainer of training group {name}', async ({apiAs}, lastName: string, groupName: string) => {
    const admin = apiAs('admin');
    await createTrainingGroup(admin, groupName, await findMemberPath(admin, lastName));
});

When('{persona} registers an adult member with last name {name}', async ({page, actAs}, persona: Persona, lastName: string) => {
    const body = registrationBody(lastName);
    await actAs(persona);
    await openFromMenu(page, 'Členové');
    await fillRegistrationForm(page, lastName, body);
    await submit(page);
    await expect(heading(page, `${body.firstName} ${lastName}`)).toBeVisible();
});

When('{persona} registers a minor member with last name {name} and legal guardian {name}', async ({page, actAs}, persona: Persona, lastName: string, guardian: string) => {
    const body = registrationBody(lastName, minorDateOfBirth());
    await actAs(persona);
    await openFromMenu(page, 'Členové');
    await fillRegistrationForm(page, lastName, body, false);
    await chooseExistingGuardian(page, guardian);
    await submit(page);
    await expect(heading(page, `${body.firstName} ${lastName}`)).toBeVisible();
});

When('{persona} registers a minor member with last name {name} and a new legal guardian {name}', async ({page, actAs}, persona: Persona, lastName: string, guardian: string) => {
    const body = registrationBody(lastName, minorDateOfBirth());
    await actAs(persona);
    await openFromMenu(page, 'Členové');
    await fillRegistrationForm(page, lastName, body, false);
    await addNewGuardian(page, guardian);
    await submit(page);
    await expect(heading(page, `${body.firstName} ${lastName}`)).toBeVisible();
});

When('{persona} opens the detail of member {name}', async ({page, actAs}, persona: Persona, lastName: string) => {
    await openDetailAs(page, actAs, persona, lastName);
});

When('{persona} changes the phone number in the profile to {string}', async ({page, actAs}, persona: Persona, phone: string) => {
    await actAs(persona);
    await openOwnProfile(page);
    await click(page, 'Upravit profil');
    await fill(page, 'Telefon', phone);
    await submit(page, 'Uložit změny');
});

When('{persona} suspends member {name}', async ({page, actAs}, persona: Persona, lastName: string) => {
    await actAs(persona);
    await openMemberDetail(page, lastName);
    await click(page, 'Ukončit členství');
    await select(page, 'Důvod', 'Odhlášení');
    await submit(page);
});

Then('the member list contains member {name}', async ({page}, lastName: string) => {
    await searchMembers(page, lastName);
    await expect(memberRow(page, lastName).first()).toBeVisible();
});

Then('{persona}\'s profile shows the phone number {string}', async ({page, currentRole}, persona: Persona, phone: string) => {
    expectActing(currentRole, persona);
    await expect(page.getByText(phone, {exact: true}).first()).toBeVisible();
});

Then('member {name} is suspended', async ({page}, lastName: string) => {
    await openMemberDetail(page, lastName, 'Vše');
    await expect(page.getByText('Neaktivní', {exact: true}).first()).toBeVisible();
});

Then('the detail of member {name} lists {name} as legal guardian', async ({page}, lastName: string, guardian: string) => {
    await openMemberDetail(page, lastName, 'Vše');
    await expect(heading(page, 'ZÁKONNÍ ZÁSTUPCI')).toBeVisible();
    await expect(page.getByText(guardian).first()).toBeVisible();
});

const WARNING = 'Varování: Správce skupin';

const groupRow = (warning: Locator, groupName: string) => warning.getByRole('listitem').filter({hasText: groupName});

const startSuspension = async (page: Page, firstName: string, lastName: string) => {
    await openMemberDetailOf(page, firstName, lastName);
    await click(page, 'Ukončit členství');
    await select(page, 'Důvod', 'Odhlášení');
    await submit(page);
    return page.getByRole('dialog', {name: WARNING});
};

const designateSuccessor = async (page: Page, warning: Locator, groupName: string, successor: string, ownersEndpoint: string) => {
    await action(groupRow(warning, groupName), 'Určit nástupce').click();
    const member = page.getByLabel(/^Člen ?\*?$/);
    await expect(member).toBeEnabled();
    const label = await member.getByRole('option', {name: new RegExp(successor)}).textContent();
    await member.selectOption({label: label!});
    const added = page.waitForResponse((response) => response.url().includes(ownersEndpoint) && response.request().method() === 'POST');
    await action(page.getByRole('dialog').last(), 'Odeslat').click();
    expect((await added).ok()).toBe(true);
    await expect(groupRow(warning, groupName).getByText('Vyřešeno')).toBeVisible();
};

When('{persona} suspends member {name} designating member {name} as successor of training group {name}', async ({page, actAs}, persona: Persona, lastName: string, successor: string, groupName: string) => {
    await actAs(persona);
    const warning = await startSuspension(page, REGISTERED_FIRST_NAME, lastName);
    await designateSuccessor(page, warning, groupName, successor, '/trainers');
    await submit(page, 'Ukončit členství');
});

const suspendWithWholeGroup = async (page: Page, lastName: string) => {
    const warning = await startSuspension(page, REGISTERED_FIRST_NAME, lastName);
    await action(warning.getByRole('listitem').filter({hasText: 'Skupina zákonných zástupců'}), 'Pozastavit celou skupinu').click();
    await action(page.getByRole('dialog').last(), 'Potvrdit').click();
    await expect(warning.getByText('Skupina bude pozastavena společně s členem')).toBeVisible();
    return warning;
};

When('{persona} suspends member {name} together with the whole legal guardian group', async ({page, actAs}, persona: Persona, lastName: string) => {
    await actAs(persona);
    await suspendWithWholeGroup(page, lastName);
    await submit(page, 'Ukončit členství');
});

Then('these members are suspended because of {string}:', async ({page}, reason: string, table: DataTable) => {
    for (const [name] of table.raw()) {
        await openMemberDetail(page, `${scenarioPrefix()}${name}`, 'Vše');
        await expect(heading(page, 'DEAKTIVACE')).toBeVisible();
        await expect(section(page, 'DEAKTIVACE').getByText(reason, {exact: true})).toBeVisible();
    }
});
