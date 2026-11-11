import {expect} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';
import {findMemberPath, registerMinorWithNewGuardian, registrationBody, REGISTERED_FIRST_NAME} from '../support/membersApi';
import {type Persona} from '../support/personas';
import {click, heading, submit} from '../support/ui';
import {fillRegistrationDetails, openMemberDetail} from './members.steps';

const {Given, When, Then} = createBdd(test);

Given('non-member guardian {name} of minor {name} exists', async ({apiAs, guardians}, guardian: string, minor: string) => {
    await registerMinorWithNewGuardian(apiAs('admin'), minor, guardian);
    guardians[guardian] = {minor};
});

When('{persona} registers guardian {name} as a member from the guardian\'s profile', async ({page, actAs, guardians}, persona: Persona, guardian: string) => {
    await actAs(persona);
    await openMemberDetail(page, guardians[guardian].minor, 'Vše');
    await page.getByRole('button', {name: new RegExp(`${REGISTERED_FIRST_NAME} ${guardian}$`)}).click();
    await click(page, 'Registrovat jako člena');
    await fillRegistrationDetails(page, registrationBody(guardian), false);
    await submit(page);
});

Then('the detail of member {name} shows a new registration number', async ({page, apiAs}, lastName: string) => {
    await expect(heading(page, `${REGISTERED_FIRST_NAME} ${lastName}`)).toBeVisible();
    const admin = apiAs('admin');
    const member = await admin.get(await findMemberPath(admin, lastName)) as {registrationNumber: string};
    expect(member.registrationNumber).not.toMatch(/^EXT\d{4}$/);
    await expect(page.getByText(member.registrationNumber, {exact: true}).first()).toBeVisible();
});
