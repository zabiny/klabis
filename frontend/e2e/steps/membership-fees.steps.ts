import {expect} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {localIsoDate, toDisplayDate} from '../support/dates';
import {test, type ApiClient, type FeeScenarioState, type HalResource} from '../support/fixtures';

const {Given, When, Then} = createBdd(test);

type Linked = HalResource & {
    _embedded?: Record<string, Array<Record<string, any>>>; // eslint-disable-line @typescript-eslint/no-explicit-any
    [key: string]: any; // eslint-disable-line @typescript-eslint/no-explicit-any
};

// membership-fees.feature asserts the voting deadline as "{date+30}".
const VOTING_DEADLINE_DAYS = 30;
const CATALOG_TIER_FEE = 1500;
// membership-fees.feature asserts "1000 CZK" for the first tier of a campaign.
const campaignTierFee = (index: number) => 1000 * (index + 1);

const embeddedList = (resource: Linked) => Object.values(resource._embedded ?? {})[0] ?? [];

const feeRoot = async (admin: ApiClient): Promise<Linked> =>
    admin.follow(await admin.get('/api'), 'membership-fees');

const closeActiveCampaign = async (admin: ApiClient) => {
    const root = await feeRoot(admin);
    if (root._links?.activeCampaign) {
        const campaign = await admin.follow(root, 'activeCampaign');
        await admin.post(admin.templateTarget(campaign, 'closeCampaign'), {});
    }
};

const nextFreeCampaignYear = async (admin: ApiClient) => {
    const root = await feeRoot(admin);
    const pastHref = root._links?.pastCampaigns?.href;
    const past: Linked = pastHref ? await admin.get(`${pastHref}&size=500`) : {};
    const years = embeddedList(past).map((campaign) => campaign.year as number);
    return Math.max(new Date().getFullYear(), ...years) + 1;
};

const ensureTier = async (admin: ApiClient, name: string, yearlyFeeAmount: number) => {
    const root = await feeRoot(admin);
    const existing = embeddedList(await admin.get(`${root._links!.self.href}?size=500`)).find((tier) => tier.name === name);
    if (existing) return existing.id as string;
    await admin.post(admin.templateTarget(root, 'createTier'), {name, yearlyFeeAmount});
    const created = embeddedList(await admin.get(`${root._links!.self.href}?size=500`)).find((tier) => tier.name === name);
    return created!.id as string;
};

const publishCampaign = async (admin: ApiClient, feeState: FeeScenarioState, tierNames: string[]) => {
    await closeActiveCampaign(admin);
    const year = await nextFreeCampaignYear(admin);
    const levelIds = [];
    for (const [index, name] of tierNames.entries()) {
        levelIds.push(await ensureTier(admin, name, campaignTierFee(index)));
    }
    const root = await feeRoot(admin);
    await admin.post(admin.templateTarget(root, 'publishYear'), {
        year,
        votingDeadline: localIsoDate(VOTING_DEADLINE_DAYS),
        levelIds,
    });
    feeState.year = year;
    const campaign = await admin.follow(await feeRoot(admin), 'activeCampaign');
    feeState.campaignPath = new URL(campaign._links!.self.href, 'http://placeholder').pathname.replace(/^\/api/, '');
};

const memberProfileHref = async (member: ApiClient) =>
    (await member.get('/api'))._links!.profile.href;

const campaignGroups = async (admin: ApiClient, year: number) => {
    const campaigns = await admin.get('/api/fee-selection-campaigns');
    const levelsHref = `${campaigns._links!.self.href}/${year}/levels`;
    return embeddedList(await admin.get(levelsHref));
};

Given('no fee selection campaign is active', async ({apiAs, feeState}) => {
    const admin = apiAs('admin');
    await closeActiveCampaign(admin);
    feeState.year = await nextFreeCampaignYear(admin);
});

Given('an active fee selection campaign for tier {text}', async ({apiAs, feeState}, tier: string) => {
    await publishCampaign(apiAs('admin'), feeState, [tier]);
});

Given('an active fee selection campaign for tiers {text} and {text}', async ({apiAs, feeState}, first: string, second: string) => {
    await publishCampaign(apiAs('admin'), feeState, [first, second]);
});

Given('a closed fee selection campaign for tier {text}', async ({apiAs, feeState}, tier: string) => {
    const admin = apiAs('admin');
    await publishCampaign(admin, feeState, [tier]);
    await closeActiveCampaign(admin);
    feeState.closedYear = feeState.year;
});

Given('the fee selection campaign is closed', async ({apiAs, feeState}) => {
    await closeActiveCampaign(apiAs('admin'));
    feeState.closedYear = feeState.year;
});

Given('a catalog fee tier {text} exists', async ({apiAs}, name: string) => {
    await ensureTier(apiAs('admin'), name, CATALOG_TIER_FEE);
});

Given('a catalog fee tier {text} with a percentage rule for ranking {text} exists', async ({apiAs}, name: string, ranking: string) => {
    const admin = apiAs('admin');
    const tierId = await ensureTier(admin, name, CATALOG_TIER_FEE);
    const tier = await admin.get(`/api/membership-fee-tiers/${tierId}`);
    const rules = await admin.follow(tier, 'rules');
    const raceType = (rules._templates!.addRule as any).properties // eslint-disable-line @typescript-eslint/no-explicit-any
        .find((property: {name: string}) => property.name === 'eventTypeId').options.inline[0].value;
    await admin.post(admin.templateTarget(rules, 'addRule'), {
        eventTypeId: raceType,
        rankingShortName: ranking,
        ruleType: 'PERCENTAGE',
        percentage: 50,
    });
});

Given('the member has chosen tier {text}', async ({apiAs, feeState}, tier: string) => {
    const member = apiAs('member');
    const summary = await member.get(`${await memberProfileHref(member)}/fee-summary/${feeState.year}`) as Linked;
    const option = (summary._templates!.chooseTier as any).properties[0].options.inline // eslint-disable-line @typescript-eslint/no-explicit-any
        .find((candidate: {prompt: string}) => candidate.prompt === tier);
    await member.post(member.templateTarget(summary, 'chooseTier'), {membershipFeeGroupId: option.value});
});

Given('the member has cancelled the tier choice', async ({apiAs, feeState}) => {
    const member = apiAs('member');
    await member.delete(`${await memberProfileHref(member)}/fee-choice/${feeState.year}`);
});

Given('the administrator has assigned the member to tier {text}', async ({apiAs, feeState}, tier: string) => {
    const admin = apiAs('admin');
    const memberHref = await memberProfileHref(apiAs('member'));
    const group = (await campaignGroups(admin, feeState.year!)).find((candidate) => candidate.name === tier)!;
    await admin.post(admin.templateTarget(group as HalResource, 'assignMember'), {
        memberId: memberHref.split('/').pop(),
        year: feeState.year,
    });
});

Given('a published event {text} with open registration exists', async ({apiAs}, name: string) => {
    const admin = apiAs('admin');
    const events = await admin.follow(await admin.get('/api'), 'events');
    await admin.post(admin.templateTarget(events, 'createEvent'), {
        name,
        eventDate: localIsoDate(60),
        organizer: 'ZBM',
        siUsage: 'NOT_USED',
        deadlines: [localIsoDate(VOTING_DEADLINE_DAYS)],
        categories: [{name: 'M21'}],
    });
    const created = embeddedList(await admin.get(`${events._links!.self.href}?q=${encodeURIComponent(name)}`))
        .find((event) => event.name === name)
        ?? embeddedList(await admin.get(`${events._links!.self.href}?size=500`)).find((event) => event.name === name)!;
    const event = await admin.get(`/api/events/${created.id}`);
    await admin.post(admin.templateTarget(event, 'publishEvent'), {});
});

Given('the member is registered for event {text}', async ({apiAs}, name: string) => {
    const member = apiAs('member');
    const events = await member.follow(await member.get('/api'), 'events');
    const found = embeddedList(await member.get(`${events._links!.self.href}?size=500`)).find((event) => event.name === name)!;
    const event = await member.get(`/api/events/${found.id}`);
    const categoryId = (event._templates!.registerForEvent as any).properties // eslint-disable-line @typescript-eslint/no-explicit-any
        .find((property: {name: string}) => property.name === 'categoryId').options.inline[0].value;
    await member.post(member.templateTarget(event, 'registerForEvent'), {categoryId});
});

When('I fill {text} with the scenario year', async ({page, feeState}, label: string) => {
    await page.getByLabel(new RegExp(`^${label}\\*?$`)).fill(String(feeState.year));
});

When('I open the catalog tier {text}', async ({page}, name: string) => {
    await page.getByRole('table', {name: 'Tabulka dat'}).getByRole('cell', {name, exact: true}).click();
});

When('I open the closed campaign', async ({page, feeState}) => {
    await page.getByRole('link', {name: String(feeState.closedYear), exact: true}).first().click();
});

const section = (page: import('@playwright/test').Page, heading: string) =>
    page.getByRole('heading', {name: heading, exact: true}).locator('xpath=..');

Then(/^the (active|past) campaigns section (contains|does not contain) the (campaign|closed campaign) year$/,
    async ({page, feeState}, kind: string, presence: string, which: string) => {
        const target = section(page, kind === 'active' ? 'Aktivní kampaň' : 'Minulé kampaně');
        const year = which === 'campaign' ? feeState.year : feeState.closedYear;
        const entry = target.getByText(String(year), {exact: true});
        if (presence === 'contains') {
            await expect(entry.first()).toBeVisible();
        } else {
            await expect(entry).toHaveCount(0);
        }
    });

Then('the active campaign lists fee group {text}', async ({page}, name: string) => {
    await expect(section(page, 'Skupiny').getByText(name, {exact: true})).toBeVisible();
});

Then('I do not see the date {text}', async ({page}, iso: string) => {
    await expect(page.getByText(toDisplayDate(iso), {exact: true})).toHaveCount(0);
});

Then('I see the date {text}', async ({page}, iso: string) => {
    await expect(page.getByText(toDisplayDate(iso), {exact: true}).first()).toBeVisible();
});

When('I navigate in the application to the scenario campaign', async ({page, feeState}) => {
    await page.evaluate((target) => {
        window.history.pushState({}, '', target);
        window.dispatchEvent(new PopStateEvent('popstate'));
    }, feeState.campaignPath);
});

When('I navigate in the application to my fee choice page', async ({page, apiAs, feeState}) => {
    const memberId = (await memberProfileHref(apiAs('member'))).split('/').pop();
    await page.evaluate((target) => {
        window.history.pushState({}, '', target);
        window.dispatchEvent(new PopStateEvent('popstate'));
    }, `/members/${memberId}/fee-choice/${feeState.year}`);
});

Then(/^the event registrations (contain|do not contain) "(.*)"$/, async ({page}, presence: string, text: string) => {
    const table = page.getByRole('table').filter({hasText: 'Příjmení'});
    await expect(table).toBeVisible();
    if (presence === 'contain') {
        await expect(table.getByText(text).first()).toBeVisible();
    } else {
        await expect(table.getByText(text)).toHaveCount(0);
    }
});

Then(/^the account (shows|does not show) the yearly membership fee of the scenario year$/,
    async ({page, feeState}, presence: string) => {
        await expect(page.getByText('Zůstatek').first()).toBeVisible();
        const charge = page.getByText(`Roční členský příspěvek ${feeState.year}`, {exact: true});
        if (presence === 'shows') {
            await expect(charge).toBeVisible();
        } else {
            await expect(charge).toHaveCount(0);
        }
    });
