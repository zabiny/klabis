import {localIsoDate} from './dates';
import type {ApiClient, FeeScenarioState} from './fixtures';
import {embeddedList, type Linked, type MemberIdentity, templateOptions} from './halApi';
import type {Contribution} from './parameters';

export type CampaignLevel = {name: string; yearlyFee?: number};

const DEFAULT_YEARLY_FEE = 1500;
const DEFAULT_VOTING_DEADLINE = localIsoDate(30);

// The fee root is the whole tier catalog, so it is fetched once per helper and passed on.
const feeRoot = async (admin: ApiClient): Promise<Linked> =>
    admin.follow(await admin.root(), 'membership-fees');

// Returns whether a campaign was closed, which changes the fee root's links and templates.
const closeListedCampaign = async (admin: ApiClient, root: Linked, feeState: FeeScenarioState) => {
    if (!root._links?.activeCampaign) return false;
    const campaign = await admin.follow(root, 'activeCampaign');
    await admin.post(admin.templateTarget(campaign, 'closeCampaign'), {});
    feeState.closedYear = feeState.year;
    return true;
};

export const closeActiveCampaign = async (admin: ApiClient, feeState: FeeScenarioState) => {
    await closeListedCampaign(admin, await feeRoot(admin), feeState);
};

// Campaign years are unique and closed campaigns are never removed, so every scenario publishes a year never used before.
export const nextFreeCampaignYear = async (admin: ApiClient, root?: Linked) => {
    const pastHref = (root ?? await feeRoot(admin))._links?.pastCampaigns?.href;
    const past: Linked = pastHref ? await admin.get(pastHref) : {};
    const years = embeddedList(past).map((campaign) => campaign.year as number);
    return Math.max(new Date().getFullYear(), ...years) + 1;
};

// Returns the fee root as it is once the year is reserved.
export const reserveCampaignYear = async (admin: ApiClient, feeState: FeeScenarioState) => {
    let root = await feeRoot(admin);
    if (await closeListedCampaign(admin, root, feeState)) {
        root = await feeRoot(admin);
    }
    feeState.year = await nextFreeCampaignYear(admin, root);
    return root;
};

const ensureLevel = async (admin: ApiClient, root: Linked, name: string, yearlyFeeAmount = DEFAULT_YEARLY_FEE) => {
    const existing = embeddedList(root).find((tier) => tier.name === name);
    if (existing) return existing.id as string;
    return (await admin.create(admin.templateTarget(root, 'createTier'), {name, yearlyFeeAmount})).split('/').pop()!;
};

export const ensureCatalogLevel = async (admin: ApiClient, name: string, yearlyFeeAmount?: number) =>
    ensureLevel(admin, await feeRoot(admin), name, yearlyFeeAmount);

export const addCatalogRule = async (admin: ApiClient, levelName: string, contribution: Contribution, ranking: string) => {
    const tier = await admin.get(`/api/membership-fee-tiers/${await ensureCatalogLevel(admin, levelName)}`);
    const rules = await admin.follow(tier, 'rules') as Linked;
    await admin.post(admin.templateTarget(rules, 'addRule'), {
        eventTypeId: templateOptions(rules, 'addRule', 'eventTypeId')[0].value,
        rankingShortName: ranking,
        ruleType: contribution.ruleType,
        ...(contribution.ruleType === 'PERCENTAGE'
            ? {percentage: contribution.value}
            : {fixedAmount: contribution.value, fixedCurrency: 'CZK'}),
    });
};

export const publishCampaign = async (
    admin: ApiClient, feeState: FeeScenarioState, levels: CampaignLevel[], votingDeadline = DEFAULT_VOTING_DEADLINE,
) => {
    const root = await reserveCampaignYear(admin, feeState);
    const levelIds = [];
    for (const level of levels) {
        levelIds.push(await ensureLevel(admin, root, level.name, level.yearlyFee));
    }
    const campaign = await admin.create(admin.templateTarget(root, 'publishYear'), {year: feeState.year, votingDeadline, levelIds});
    feeState.campaignPath = campaign.replace(/^\/api/, '');
};

const publishedLevels = async (admin: ApiClient, feeState: FeeScenarioState) => {
    if (!feeState.campaignPath) throw new Error('membershipFeesApi: no campaign published through the API in this scenario');
    const campaign = await admin.get(`/api${feeState.campaignPath}`);
    return embeddedList(await admin.follow(campaign, 'levels')) as Linked[];
};

const feeSummary = async (member: ApiClient, year: number) =>
    member.get(`${(await member.root())._links!.profile.href}/fee-summary/${year}`) as Promise<Linked>;

export const chooseLevel = async (member: ApiClient, year: number, levelName: string) => {
    const summary = await feeSummary(member, year);
    const option = templateOptions(summary, 'chooseTier', 'membershipFeeGroupId').find((candidate) => candidate.prompt === levelName)!;
    await member.post(member.templateTarget(summary, 'chooseTier'), {membershipFeeGroupId: option.value});
};

export const withdrawChoice = async (member: ApiClient, year: number) => {
    const summary = await feeSummary(member, year);
    if (summary._templates?.removeChoice) {
        await member.delete(member.templateTarget(summary, 'removeChoice'));
    }
};

export const assignLevel = async (admin: ApiClient, member: MemberIdentity, feeState: FeeScenarioState, levelName: string) => {
    const level = (await publishedLevels(admin, feeState)).find((candidate) => candidate.name === levelName)!;
    await admin.post(admin.templateTarget(level, 'assignMember'), {memberId: member.id, year: feeState.year});
};

export const firstPublishedLevelName = async (admin: ApiClient, feeState: FeeScenarioState) =>
    (await publishedLevels(admin, feeState))[0].name as string;
