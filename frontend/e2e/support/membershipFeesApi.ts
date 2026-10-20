import {localIsoDate} from './dates';
import type {ApiClient, FeeScenarioState} from './fixtures';
import {embeddedList, type Linked, type MemberIdentity, templateOptions} from './halApi';
import type {Contribution} from './parameters';

export type CampaignLevel = {name: string; yearlyFee?: number};

const DEFAULT_YEARLY_FEE = 1500;
const DEFAULT_VOTING_DEADLINE = localIsoDate(30);

const feeRoot = async (admin: ApiClient): Promise<Linked> =>
    admin.follow(await admin.get('/api'), 'membership-fees');

export const closeActiveCampaign = async (admin: ApiClient, feeState: FeeScenarioState) => {
    const root = await feeRoot(admin);
    if (!root._links?.activeCampaign) return;
    const campaign = await admin.follow(root, 'activeCampaign');
    await admin.post(admin.templateTarget(campaign, 'closeCampaign'), {});
    feeState.closedYear = feeState.year;
};

// Campaign years are unique and closed campaigns are never removed, so every scenario publishes a year never used before.
export const nextFreeCampaignYear = async (admin: ApiClient) => {
    const root = await feeRoot(admin);
    const pastHref = root._links?.pastCampaigns?.href;
    const past: Linked = pastHref ? await admin.get(`${pastHref}&size=500`) : {};
    const years = embeddedList(past).map((campaign) => campaign.year as number);
    return Math.max(new Date().getFullYear(), ...years) + 1;
};

const findTier = async (admin: ApiClient, name: string) => {
    const root = await feeRoot(admin);
    return embeddedList(await admin.get(`${root._links!.self.href}?size=500`)).find((tier) => tier.name === name);
};

export const ensureCatalogLevel = async (admin: ApiClient, name: string, yearlyFeeAmount = DEFAULT_YEARLY_FEE) => {
    const existing = await findTier(admin, name);
    if (existing) return existing.id as string;
    await admin.post(admin.templateTarget(await feeRoot(admin), 'createTier'), {name, yearlyFeeAmount});
    return (await findTier(admin, name))!.id as string;
};

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

export const reserveCampaignYear = async (admin: ApiClient, feeState: FeeScenarioState) => {
    await closeActiveCampaign(admin, feeState);
    feeState.year = await nextFreeCampaignYear(admin);
};

const rememberActiveCampaign = async (admin: ApiClient, feeState: FeeScenarioState) => {
    const campaign = await admin.follow(await feeRoot(admin), 'activeCampaign');
    feeState.campaignPath = new URL(campaign._links!.self.href, 'http://placeholder').pathname.replace(/^\/api/, '');
};

export const publishCampaign = async (
    admin: ApiClient, feeState: FeeScenarioState, levels: CampaignLevel[], votingDeadline = DEFAULT_VOTING_DEADLINE,
) => {
    await reserveCampaignYear(admin, feeState);
    const levelIds = [];
    for (const level of levels) {
        levelIds.push(await ensureCatalogLevel(admin, level.name, level.yearlyFee));
    }
    await admin.post(admin.templateTarget(await feeRoot(admin), 'publishYear'), {year: feeState.year, votingDeadline, levelIds});
    await rememberActiveCampaign(admin, feeState);
};

const publishedLevels = async (admin: ApiClient, year: number) => {
    const campaigns = await admin.get('/api/fee-selection-campaigns');
    return embeddedList(await admin.get(`${campaigns._links!.self.href}/${year}/levels`)) as Linked[];
};

const feeSummary = async (member: ApiClient, year: number) =>
    member.get(`${(await member.get('/api'))._links!.profile.href}/fee-summary/${year}`) as Promise<Linked>;

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

export const assignLevel = async (admin: ApiClient, member: MemberIdentity, year: number, levelName: string) => {
    const level = (await publishedLevels(admin, year)).find((candidate) => candidate.name === levelName)!;
    await admin.post(admin.templateTarget(level, 'assignMember'), {memberId: member.id, year});
};

export const firstPublishedLevelName = async (admin: ApiClient, year: number) =>
    (await publishedLevels(admin, year))[0].name as string;
