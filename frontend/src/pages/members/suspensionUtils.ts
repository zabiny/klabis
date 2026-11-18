import {ApiError} from "../../api/apiError.ts";
import type {components} from "../../api/klabisApi";

export type AffectedGroup = components["schemas"]["AffectedGroup"];

export interface NegativeBalanceWarning {
    balance: { amount: number; currency: string };
    accountLink: string;
}

export type DependentMemberWarning = Omit<components["schemas"]["DependentMemberWarning"], 'debt' | 'groups'> & {
    debt?: NegativeBalanceWarning;
    groups?: AffectedGroup[];
};

export interface SuspensionBlockedWarning {
    groups: AffectedGroup[] | null;
    debt: NegativeBalanceWarning | null;
    dependentMembers?: DependentMemberWarning[];
}

export const parseSuspensionBlockedWarning409 = (error: unknown): SuspensionBlockedWarning | null => {
    if (!(error instanceof ApiError) || error.status !== 409) return null;
    const {groups: rawGroups, debt: rawDebt, dependentMembers: rawDependents} = error.params as {
        dependentMembers?: unknown;
        groups?: {affectedGroups?: unknown};
        debt?: {balance?: {amount?: unknown}; accountLink?: unknown};
    };

    const groups = Array.isArray(rawGroups?.affectedGroups)
        ? (rawGroups.affectedGroups as AffectedGroup[])
        : null;

    const debt =
        rawDebt && typeof rawDebt.balance?.amount === 'number' && typeof rawDebt.accountLink === 'string'
            ? (rawDebt as NegativeBalanceWarning)
            : null;

    const dependentMembers = Array.isArray(rawDependents) && rawDependents.length > 0
        ? (rawDependents as Array<Omit<DependentMemberWarning, 'groups'> & {groups?: {affectedGroups?: AffectedGroup[]}}>)
            .map(({groups: dependentGroups, ...rest}) => ({...rest, groups: dependentGroups?.affectedGroups}))
        : undefined;

    return groups || debt || dependentMembers ? {groups, debt, dependentMembers} : null;
};
