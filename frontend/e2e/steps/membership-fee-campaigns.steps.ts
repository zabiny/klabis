import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test, type ApiClient, type FeeScenarioState} from '../support/fixtures';
import {closeActiveCampaign, nextFreeCampaignYear, publishCampaign, reserveCampaignYear} from '../support/membershipFeesApi';
import {
    ACTIVE_CAMPAIGN,
    CATALOG,
    LEVELS,
    openClosedCampaign,
    openFeesPage,
    openListedClosedCampaign,
    openListedLevel,
    PAST_CAMPAIGNS,
} from '../support/membershipFeesPages';
import {expectActing, type Persona} from '../support/personas';
import {
    action,
    click,
    displayedDate,
    expectRefused,
    fill,
    heading,
    navigateInApp,
    pick,
    section,
    submit,
} from '../support/ui';

const {Given, When, Then} = createBdd(test);

const startCampaign = async (page: Page, feeState: FeeScenarioState, admin: ApiClient, levels: string[], deadline: string) => {
    feeState.year ??= await nextFreeCampaignYear(admin);
    await openFeesPage(page);
    await click(page, 'Vypsat kampaň');
    await fill(page, 'Rok', String(feeState.year));
    await fill(page, 'Uzávěrka hlasování', deadline);
    for (const level of levels) {
        await pick(page, 'Tiery', level);
    }
    await submit(page);
};

const campaignYear = (page: Page, sectionName: string, year: number | undefined) =>
    section(page, sectionName).getByText(String(year), {exact: true});

Given('no fee selection campaign is active', async ({apiAs, feeState}) => {
    await reserveCampaignYear(apiAs('admin'), feeState);
});

Given('an active fee selection campaign offers level {name}', async ({apiAs, feeState}, name: string) => {
    await publishCampaign(apiAs('admin'), feeState, [{name}]);
});

Given('an active fee selection campaign offers levels {name} and {name}', async ({apiAs, feeState}, first: string, second: string) => {
    await publishCampaign(apiAs('admin'), feeState, [{name: first}, {name: second}]);
});

Given('an active fee selection campaign offers level {name} with yearly fee {int} CZK',
    async ({apiAs, feeState}, name: string, yearlyFee: number) => {
        await publishCampaign(apiAs('admin'), feeState, [{name, yearlyFee}]);
    });

Given('an active fee selection campaign offers level {name} with the voting deadline {day}',
    async ({apiAs, feeState}, name: string, deadline: string) => {
        await publishCampaign(apiAs('admin'), feeState, [{name}], deadline);
    });

Given('a closed fee selection campaign that offered level {name}', async ({apiAs, feeState}, name: string) => {
    const admin = apiAs('admin');
    await publishCampaign(admin, feeState, [{name}]);
    await closeActiveCampaign(admin, feeState);
});

Given('the fee selection campaign is closed', async ({apiAs, feeState}) => {
    await closeActiveCampaign(apiAs('admin'), feeState);
});

// No API represents a page being shown, so this state is reached through the UI.
Given('{persona} is viewing the closed campaign', async ({page, actAs, feeState}, persona: Persona) => {
    await actAs(persona);
    await openClosedCampaign(page, feeState);
});

When('{persona} starts a fee selection campaign offering level {name} with the voting deadline {day}',
    async ({page, actAs, apiAs, feeState}, persona: Persona, name: string, deadline: string) => {
        await actAs(persona);
        await startCampaign(page, feeState, apiAs('admin'), [name], deadline);
    });

When('{persona} starts a fee selection campaign offering levels {name} and {name} with the voting deadline {day}',
    async ({page, actAs, apiAs, feeState}, persona: Persona, first: string, second: string, deadline: string) => {
        await actAs(persona);
        await startCampaign(page, feeState, apiAs('admin'), [first, second], deadline);
    });

When('{persona} opens the membership fees page', async ({page, actAs}, persona: Persona) => {
    await actAs(persona);
    await openFeesPage(page);
});

// Members have no link to the campaign detail, so it is reached through the SPA router.
When('{persona} opens the active campaign', async ({page, actAs, feeState}, persona: Persona) => {
    await actAs(persona);
    await navigateInApp(page, feeState.campaignPath!);
    await expect(heading(page, String(feeState.year))).toBeVisible();
});

When('{persona} opens the closed campaign', async ({page, actAs, feeState}, persona: Persona) => {
    await actAs(persona);
    await openClosedCampaign(page, feeState);
});

When('{persona} changes the voting deadline to {day}', async ({page, actAs}, persona: Persona, deadline: string) => {
    await actAs(persona);
    await openFeesPage(page);
    await click(page, 'Změnit deadline');
    await fill(page, 'Uzávěrka hlasování', deadline);
    await submit(page);
});

When('{persona} closes the active campaign', async ({page, actAs, feeState}, persona: Persona) => {
    await actAs(persona);
    await openFeesPage(page);
    await click(page, 'Uzavřít kampaň');
    if ((await submit(page)).ok()) {
        feeState.closedYear = feeState.year;
    }
});

When('the fee selection campaign ends', async ({apiAs, feeState}) => {
    await closeActiveCampaign(apiAs('admin'), feeState);
});

for (const request of ['rule', 'campaign', 'deadline change']) {
    Then(`the ${request} is refused`, async ({page}) => {
        await expectRefused(page);
    });
}

Then('the membership fees page shows no active campaign', async ({page}) => {
    await openFeesPage(page);
    await expect(heading(page, ACTIVE_CAMPAIGN)).toHaveCount(0);
});

Then('the membership fees page shows no past campaigns', async ({page}) => {
    await openFeesPage(page);
    await expect(heading(page, PAST_CAMPAIGNS)).toHaveCount(0);
});

Then('the membership fees page shows the active campaign of the campaign year', async ({page, feeState}) => {
    await openFeesPage(page);
    await expect(campaignYear(page, ACTIVE_CAMPAIGN, feeState.year)).toBeVisible();
});

Then('the membership fees page lists the closed campaign among the past campaigns', async ({page, feeState}) => {
    await openFeesPage(page);
    await expect(campaignYear(page, PAST_CAMPAIGNS, feeState.closedYear).first()).toBeVisible();
});

Then('the membership fees page does not list the active campaign among the past campaigns', async ({page, feeState}) => {
    await openFeesPage(page);
    await expect(campaignYear(page, ACTIVE_CAMPAIGN, feeState.year)).toBeVisible();
    await expect(campaignYear(page, PAST_CAMPAIGNS, feeState.year)).toHaveCount(0);
});

Then('the membership fees page is shown', async ({page}) => {
    await expect(heading(page, CATALOG)).toBeVisible();
    await expect(heading(page, PAST_CAMPAIGNS)).toBeVisible();
});

Then('{persona} is not offered to start a fee selection campaign', async ({page, currentRole}, persona: Persona) => {
    expectActing(currentRole, persona);
    await openFeesPage(page);
    await expect(heading(page, ACTIVE_CAMPAIGN)).toBeVisible();
    await expect(action(page, 'Vypsat kampaň')).toHaveCount(0);
});

Then('the active campaign has the voting deadline {day}', async ({page}, deadline: string) => {
    await openFeesPage(page);
    await expect(displayedDate(section(page, ACTIVE_CAMPAIGN), deadline)).toBeVisible();
});

Then('the active campaign offers level {name}', async ({page}, name: string) => {
    await openFeesPage(page);
    await expect(section(page, LEVELS).getByRole('link', {name, exact: true})).toBeVisible();
});

// The campaign actions are checked on the campaign the persona opened in the When step.
for (const [phrase, label] of [['change the voting deadline', 'Změnit deadline'], ['close the campaign', 'Uzavřít kampaň']]) {
    Then(`{persona} is offered to ${phrase}`, async ({page, currentRole}, persona: Persona) => {
        expectActing(currentRole, persona);
        await expect(action(page, label).first()).toBeVisible();
    });

    Then(`{persona} is not offered to ${phrase}`, async ({page, currentRole}, persona: Persona) => {
        expectActing(currentRole, persona);
        await expect(heading(page, LEVELS)).toBeVisible();
        await expect(action(page, label)).toHaveCount(0);
    });
}

Then('{persona} can open the closed campaign', async ({page, feeState, currentRole}, persona: Persona) => {
    expectActing(currentRole, persona);
    await openListedClosedCampaign(page, feeState);
});

Then('{persona} can open the published level {name}', async ({page, currentRole}, persona: Persona, name: string) => {
    expectActing(currentRole, persona);
    await openListedLevel(page, name);
});
