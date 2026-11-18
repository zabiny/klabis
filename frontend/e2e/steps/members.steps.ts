import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';
import {type Linked} from '../support/halApi';
import {minorDateOfBirth} from '../support/dates';
import {createTrainingGroup, findMemberPath, guardianEmail, registerMember, REGISTERED_FIRST_NAME, registrationBody} from '../support/membersApi';
import {expectActing, personas, type Persona} from '../support/personas';
import {action, chooseCountry, click, czechAmount, dismissDialogs, expectRefused, fill, heading, openFromMenu, select, submit} from '../support/ui';

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
    page.getByRole('button', {name: /^Row \d+$/}).filter({hasText: lastName}).filter({hasText: firstName});

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

Given('member {name} is suspended because of {string}', async ({apiAs}, lastName: string, note: string) => {
    const admin = apiAs('admin');
    const member = await admin.get(await registerMember(admin, lastName)) as Linked;
    await admin.post(admin.templateTarget(member, 'suspendMember'), {reason: 'PRESTUP', note});
});

Given('the phone number of {persona} is {string}', async ({apiAs}, persona: Persona, phone: string) => {
    const member = apiAs(personas[persona]);
    const profile = await member.get((await member.root())._links!.profile.href) as Linked;
    await member.patch(member.templateTarget(profile, 'updateMember'), {phone});
});

Given('the address of {persona} is in {string}', async ({apiAs}, persona: Persona, country: string) => {
    const member = apiAs(personas[persona]);
    const profile = await member.get((await member.root())._links!.profile.href) as Linked;
    await member.patch(member.templateTarget(profile, 'updateMember'), {address: {...registrationBody('Eva').address, country}});
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

When('{persona} registers a minor member with last name {name} without a legal guardian', async ({page, actAs}, persona: Persona, lastName: string) => {
    await actAs(persona);
    await openFromMenu(page, 'Členové');
    await fillRegistrationForm(page, lastName, registrationBody(lastName, minorDateOfBirth()), false);
    await submit(page);
});

When('{persona} searches the member list for {name}', async ({page, actAs}, persona: Persona, lastName: string) => {
    await actAs(persona);
    await searchMembers(page, lastName);
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

When('{persona} opens the edit form of her profile', async ({page, actAs}, persona: Persona) => {
    await actAs(persona);
    await openOwnProfile(page);
    await click(page, 'Upravit profil');
    await expect(action(page, 'Uložit změny')).toBeVisible();
});

When('{persona} changes the country of her address to {string}', async ({page, actAs}, persona: Persona, code: string) => {
    await actAs(persona);
    await openOwnProfile(page);
    await click(page, 'Upravit profil');
    await chooseCountry(page, 'Stát', code);
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

Then('only member {name} is listed', async ({page}, lastName: string) => {
    await expect(memberRow(page, lastName)).toHaveCount(1);
    await expect(page.getByText('1–1 z 1')).toBeVisible();
});

Then('the detail shows the personal data and address of member {name}', async ({page}, lastName: string) => {
    const body = registrationBody(lastName);
    await expect(page.getByText(lastName, {exact: true}).first()).toBeVisible();
    await expect(page.getByText(body.address.city, {exact: true})).toBeVisible();
    await expect(page.getByText(body.address.street, {exact: true})).toBeVisible();
});

Then('{persona}\'s profile shows the phone number {string}', async ({page, currentRole}, persona: Persona, phone: string) => {
    expectActing(currentRole, persona);
    await expect(page.getByText(phone, {exact: true}).first()).toBeVisible();
});

Then('{persona}\'s nationality {string} is shown read-only', async ({page, currentRole}, persona: Persona, nationality: string) => {
    expectActing(currentRole, persona);
    await expect(page.getByLabel('Telefon')).toBeVisible();
    await expect(page.getByLabel(/^Státní příslušnost/)).toHaveCount(0);
    await expect(page.getByText('Státní příslušnost', {exact: true}).locator('xpath=..').getByText(nationality, {exact: true})).toBeVisible();
});

Then('{persona}\'s address country is {string}', async ({page, currentRole}, persona: Persona, country: string) => {
    expectActing(currentRole, persona);
    await expect(page.getByText('Stát', {exact: true}).locator('xpath=..').getByText(country, {exact: true})).toBeVisible();
});

Then('member {name} is suspended', async ({page}, lastName: string) => {
    await openMemberDetail(page, lastName, 'Vše');
    await expect(page.getByText('Neaktivní', {exact: true}).first()).toBeVisible();
});

Then('the detail of member {name} shows the suspension with note {string}', async ({page}, lastName: string, note: string) => {
    await expect(heading(page, `${REGISTERED_FIRST_NAME} ${lastName}`)).toBeVisible();
    await expect(heading(page, 'DEAKTIVACE')).toBeVisible();
    await expect(page.getByText(note, {exact: true})).toBeVisible();
});

Then('the registration is refused', async ({page}) => {
    await expectRefused(page);
});

Then('member {name} is not registered', async ({page}, lastName: string) => {
    await searchMembers(page, lastName);
    await expect(page.getByText('Žádní členové neodpovídají zadaným filtrům.')).toBeVisible();
    await expect(memberRow(page, lastName)).toHaveCount(0);
});

Then('the detail of member {name} lists {name} as legal guardian', async ({page}, lastName: string, guardian: string) => {
    await openMemberDetail(page, lastName, 'Vše');
    await expect(heading(page, 'ZÁKONNÍ ZÁSTUPCI')).toBeVisible();
    await expect(page.getByText(guardian).first()).toBeVisible();
});

Then('{persona} is not offered to edit the profile of member {name}', async ({page, currentRole}, persona: Persona, lastName: string) => {
    expectActing(currentRole, persona);
    await expect(heading(page, `${REGISTERED_FIRST_NAME} ${lastName}`)).toBeVisible();
    await expect(page.getByRole('button', {name: /^Upravit/})).toHaveCount(0);
});

Then('the suspension is blocked because member {name} is the last owner of training group {name}', async ({page}, lastName: string, groupName: string) => {
    await expect(heading(page, `${REGISTERED_FIRST_NAME} ${lastName}`)).toBeVisible();
    const warning = page.getByRole('dialog', {name: 'Varování: Správce skupin'});
    await expect(warning.getByText(groupName)).toBeVisible();
});

When('{persona} suspends member {name} designating member {name} as successor of training group {name}', async ({page, actAs}, persona: Persona, lastName: string, successor: string, groupName: string) => {
    await actAs(persona);
    await openMemberDetail(page, lastName);
    await click(page, 'Ukončit členství');
    await select(page, 'Důvod', 'Odhlášení');
    await submit(page);
    const warning = page.getByRole('dialog', {name: 'Varování: Správce skupin'});
    await action(warning.getByRole('listitem').filter({hasText: groupName}), 'Určit nástupce').click();
    const member = page.getByLabel(/^Člen ?\*?$/);
    await expect(member).toBeEnabled();
    const label = await member.getByRole('option', {name: new RegExp(successor)}).textContent();
    await member.selectOption({label: label!});
    const added = page.waitForResponse((response) => response.url().includes('/trainers') && response.request().method() === 'POST');
    await action(page.getByRole('dialog').last(), 'Odeslat').click();
    expect((await added).ok()).toBe(true);
    await expect(warning.getByText('Vyřešeno')).toBeVisible();
    await submit(page, 'Ukončit členství');
});

Then('the suspension is blocked by one warning showing the balance {int} CZK and training group {name}', async ({page}, balance: number, groupName: string) => {
    const warning = page.getByRole('dialog', {name: 'Varování: Správce skupin'});
    await expect(warning.getByText(groupName)).toBeVisible();
    await expect(warning.getByText(czechAmount(balance))).toBeVisible();
    await expect(action(warning, 'Otevřít účet')).toBeVisible();
    await expect(page.getByRole('dialog')).toHaveCount(1);
    await expect(action(warning, 'Ukončit členství')).toHaveCount(0);
});

Then('the suspension is blocked because the balance is {int} CZK', async ({page}, balance: number) => {
    const warning = page.getByRole('dialog', {name: 'Záporný zůstatek'});
    await expect(warning.getByText(czechAmount(balance))).toBeVisible();
    await expect(action(warning, 'Otevřít účet')).toBeVisible();
});

Then('member {name} is still active', async ({page}, lastName: string) => {
    await dismissDialogs(page);
    await openMemberDetail(page, lastName, 'Vše');
    await expect(page.getByText('Aktivní', {exact: true}).first()).toBeVisible();
    await expect(page.getByText('Neaktivní', {exact: true})).toHaveCount(0);
});
