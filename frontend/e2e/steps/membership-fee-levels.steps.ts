import {expect} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';
import {memberIdentity} from '../support/halApi';
import {assignLevel, chooseLevel, closeActiveCampaign, publishCampaign, withdrawChoice} from '../support/membershipFeesApi';
import {expectMembersLoaded, levelDetail, memberRow, openPublishedLevel, ruleRow} from '../support/membershipFeesPages';
import type {Contribution} from '../support/parameters';
import {personas, type Persona} from '../support/personas';
import {click, fill, select, submit} from '../support/ui';

const {Given, When, Then} = createBdd(test);

Given('{persona} has chosen level {name}', async ({apiAs, feeState}, persona: Persona, name: string) => {
    await chooseLevel(apiAs(personas[persona]), feeState.year!, name);
});

Given('{persona} has not chosen a fee level', async ({apiAs, feeState}, persona: Persona) => {
    await withdrawChoice(apiAs(personas[persona]), feeState.year!);
});

Given('{persona} has assigned {persona} to level {name}', async ({apiAs, feeState}, administrator: Persona, member: Persona, name: string) => {
    await assignLevel(apiAs(personas[administrator]), await memberIdentity(apiAs(personas[member])), feeState.year!, name);
});

Given('{persona} had level {name} in the previous fee selection campaign', async ({apiAs, feeState}, persona: Persona, name: string) => {
    const admin = apiAs('admin');
    await publishCampaign(admin, feeState, [{name}]);
    await chooseLevel(apiAs(personas[persona]), feeState.year!, name);
    await closeActiveCampaign(admin, feeState);
});

Given('{persona} is blocked for missing the fee level choice', async ({apiAs, feeState}, persona: Persona) => {
    await withdrawChoice(apiAs(personas[persona]), feeState.year!);
    await closeActiveCampaign(apiAs('admin'), feeState);
});

When('{persona} changes the yearly fee of the published level {name} to {int} CZK',
    async ({page, actAs, feeState}, persona: Persona, name: string, yearlyFee: number) => {
        await actAs(persona);
        await openPublishedLevel(page, feeState, name);
        await click(page, 'Upravit');
        await fill(page, 'Roční poplatek', String(yearlyFee));
        await submit(page, 'Uložit změny');
    });

When('{persona} assigns {persona} to level {name}',
    async ({page, actAs, apiAs, feeState}, administrator: Persona, member: Persona, name: string) => {
        const {firstName, lastName, registrationNumber} = await memberIdentity(apiAs(personas[member]));
        await actAs(administrator);
        await openPublishedLevel(page, feeState, name);
        await click(page, 'Přiřadit člena');
        await select(page, 'Člen', `${firstName} ${lastName} (${registrationNumber})`);
        await fill(page, 'Rok', String(feeState.year));
        await submit(page);
    });

Then('the published level {name} has yearly fee {int} CZK', async ({page, feeState}, name: string, yearlyFee: number) => {
    await openPublishedLevel(page, feeState, name);
    await expect(levelDetail(page).getByText(`${yearlyFee} CZK`, {exact: true})).toBeVisible();
});

Then('the published level {name} has a {contribution} rule for races of ranking {name}',
    async ({page, feeState}, name: string, contribution: Contribution, ranking: string) => {
        await openPublishedLevel(page, feeState, name);
        await expect(ruleRow(page, contribution, ranking)).toBeVisible();
    });

Then('the published level {name} lists {persona} as a member',
    async ({page, apiAs, feeState}, name: string, member: Persona) => {
        const {registrationNumber} = await memberIdentity(apiAs(personas[member]));
        await openPublishedLevel(page, feeState, name);
        await expect(memberRow(page, registrationNumber)).toBeVisible();
    });

Then('the published level {name} does not list {persona} as a member',
    async ({page, apiAs, feeState}, name: string, member: Persona) => {
        const {registrationNumber} = await memberIdentity(apiAs(personas[member]));
        await openPublishedLevel(page, feeState, name);
        await expectMembersLoaded(page);
        await expect(memberRow(page, registrationNumber)).toHaveCount(0);
    });
