import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';
import {type Linked} from '../support/halApi';
import {registerMember, REGISTERED_FIRST_NAME, registrationBody} from '../support/membersApi';
import {expectActing, personas, type Persona} from '../support/personas';
import {action, click, fill, heading, openFromMenu, select, submit} from '../support/ui';

const {Given, When, Then} = createBdd(test);

// Returning to an already open member list keeps the typed text but not its filtering, so the list is always entered from the home page.
export const searchMembers = async (page: Page, text: string, status?: string) => {
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

When('{persona} registers an adult member with last name {name}', async ({page, actAs}, persona: Persona, lastName: string) => {
    const body = registrationBody(lastName);
    await actAs(persona);
    await openFromMenu(page, 'Členové');
    await click(page, 'Registrovat člena');
    await fill(page, 'Jméno', body.firstName);
    await fill(page, 'Příjmení', lastName);
    await fill(page, 'Datum narození', body.dateOfBirth);
    await select(page, 'Pohlaví', 'Muž');
    await fill(page, 'Státní příslušnost', body.nationality);
    await fill(page, 'Rodné číslo', body.birthNumber);
    await fill(page, 'E-mail', body.email);
    await fill(page, 'Telefon', body.phone);
    await fill(page, 'Ulice', body.address.street);
    await fill(page, 'Město', body.address.city);
    await fill(page, 'PSČ', body.address.postalCode);
    await fill(page, 'Stát', body.address.country);
    await submit(page);
    await expect(heading(page, `${body.firstName} ${lastName}`)).toBeVisible();
});

When('{persona} searches the member list for {name}', async ({page, actAs}, persona: Persona, lastName: string) => {
    await actAs(persona);
    await searchMembers(page, lastName);
});

When('{persona} opens the detail of member {name}', async ({page, actAs}, persona: Persona, lastName: string) => {
    await actAs(persona);
    await openMemberDetail(page, lastName, 'Vše');
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
