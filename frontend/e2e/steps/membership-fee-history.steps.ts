import {expect} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';
import {memberIdentity} from '../support/halApi';
import {findMemberIdByName} from '../support/membersApi';
import {assignLevel} from '../support/membershipFeesApi';
import {feeHistoryButton, historyEntry, openFeeHistoryFromDetail} from '../support/membershipFeesPages';
import {expectActing, personas, type Persona} from '../support/personas';
import {heading} from '../support/ui';
import {openDetailAs, openOwnProfile} from './members.steps';

const {Given, When, Then} = createBdd(test);

const splitName = (fullName: string) => {
    const [firstName, ...rest] = fullName.split(' ');
    return {firstName, lastName: rest.join(' ')};
};

const OWN_CHOICE = 'Vlastní volba';

Given('{persona} has assigned member {string} to level {name}',
    async ({apiAs, feeState}, administrator: Persona, fullName: string, level: string) => {
        const {firstName, lastName} = splitName(fullName);
        const admin = apiAs(personas[administrator]);
        await assignLevel(admin, {id: await findMemberIdByName(admin, firstName, lastName)}, feeState, level);
    });

When('{persona} opens the fee level history from the profile', async ({page, actAs}, persona: Persona) => {
    await actAs(persona);
    await openOwnProfile(page);
    await openFeeHistoryFromDetail(page);
});

When('{persona} opens the fee level history of {persona}', async ({page, actAs, apiAs}, actor: Persona, member: Persona) => {
    const {firstName, lastName} = await memberIdentity(apiAs(personas[member]));
    await openDetailAs(page, actAs, actor, lastName, firstName);
    await openFeeHistoryFromDetail(page);
});

When('{persona} opens the fee level history of member {string}', async ({page, actAs}, persona: Persona, fullName: string) => {
    const {firstName, lastName} = splitName(fullName);
    await openDetailAs(page, actAs, persona, lastName, firstName);
    await openFeeHistoryFromDetail(page);
});

When('{persona} opens the detail of club member {string}', async ({page, actAs}, persona: Persona, fullName: string) => {
    const {firstName, lastName} = splitName(fullName);
    await openDetailAs(page, actAs, persona, lastName, firstName);
});

Then('the history shows level {name} for the campaign year', async ({page, feeState}, level: string) => {
    await expect(historyEntry(page, level, feeState.year!)).toBeVisible();
});

Then('the history shows level {name} for the campaign year as the member\'s own choice', async ({page, feeState}, level: string) => {
    await expect(historyEntry(page, level, feeState.year!).getByText(OWN_CHOICE, {exact: true})).toBeVisible();
});

Then('{persona} is not offered the fee level history of member {string}', async ({page, currentRole}, persona: Persona, fullName: string) => {
    expectActing(currentRole, persona);
    await expect(heading(page, fullName)).toBeVisible();
    await expect(feeHistoryButton(page)).toHaveCount(0);
});
