import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';
import {type Linked} from '../support/halApi';
import {minorDateOfBirth} from '../support/dates';
import {createTrainingGroup, findMemberPath, guardianEmail, registerMember, REGISTERED_FIRST_NAME, registrationBody} from '../support/membersApi';
import {expectActing, personas, type Persona} from '../support/personas';
import {action, click, czechAmount, dismissDialogs, expectRefused, fill, heading, openFromMenu, select, submit} from '../support/ui';

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
export const memberRow = (page: Page, lastName: string) =>
    page.getByRole('button', {name: /^Row \d+$/}).filter({hasText: lastName}).filter({hasText: REGISTERED_FIRST_NAME});

export const openMemberDetail = async (page: Page, lastName: string, status?: string) => {
    await searchMembers(page, lastName, status);
    await memberRow(page, lastName).getByRole('cell', {name: lastName}).first().click();
    await expect(heading(page, 'ADRESA')).toBeVisible();
};

const openOwnProfile = async (page: Page) => {
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
    // The inputs of a new guardian have no accessible names yet (a11y follow-up); they come in the order of the form.
    const [firstName, surname, email, phone] = await row.getByRole('textbox').all();
    await firstName.fill(REGISTERED_FIRST_NAME);
    await surname.fill(lastName);
    await email.fill(guardianEmail(lastName));
    await phone.fill('+420 777 333 444');
};

const fillRegistrationForm = async (page: Page, lastName: string, body: ReturnType<typeof registrationBody>, contact = true) => {
    await click(page, 'Registrovat člena');
    await fill(page, 'Jméno', body.firstName);
    await fill(page, 'Příjmení', lastName);
    await fill(page, 'Datum narození', body.dateOfBirth);
    await select(page, 'Pohlaví', 'Muž');
    await fill(page, 'Státní příslušnost', body.nationality);
    await fill(page, 'Rodné číslo', body.birthNumber);
    if (contact) {
        await fill(page, 'E-mail', body.email);
        await fill(page, 'Telefon', body.phone);
    }
    await fill(page, 'Ulice', body.address.street);
    await fill(page, 'Město', body.address.city);
    await fill(page, 'PSČ', body.address.postalCode);
    await fill(page, 'Stát', body.address.country);
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
    await actAs(persona);
    await openMemberDetail(page, lastName, personas[persona] === 'admin' ? 'Vše' : undefined);
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
    await select(page, 'Důvod zrušení (volitelné)', 'Odhlášení');
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

Then('member {name} is suspended', async ({page}, lastName: string) => {
    await openMemberDetail(page, lastName, 'Vše');
    await expect(page.getByText('Neaktivní', {exact: true}).first()).toBeVisible();
});

Then('the detail of member {name} shows the suspension with note {string}', async ({page}, _lastName: string, note: string) => {
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

Then('{persona} is not offered to edit the profile of member {name}', async ({page, currentRole}, persona: Persona, _lastName: string) => {
    expectActing(currentRole, persona);
    await expect(page.getByRole('button', {name: /^Upravit/})).toHaveCount(0);
});

Then('the suspension is blocked because member {name} is the last owner of training group {name}', async ({page}, _lastName: string, groupName: string) => {
    const warning = page.getByRole('dialog', {name: 'Varování: Správce skupin'});
    await expect(warning.getByText(groupName)).toBeVisible();
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
