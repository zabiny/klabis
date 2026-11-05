import {ApiError} from "../../api/apiError.ts";
import {type AffectedGroup} from "./SuspensionWarningDialog.tsx";

export interface NegativeBalanceWarning {
    balance: { amount: number; currency: string };
    accountLink: string;
}

export interface SuspensionBlockedWarning {
    groups: AffectedGroup[] | null;
    debt: NegativeBalanceWarning | null;
}

export const parseSuspensionBlockedWarning409 = (error: unknown): SuspensionBlockedWarning | null => {
    if (!(error instanceof ApiError) || error.status !== 409) return null;
    const {groups: rawGroups, debt: rawDebt} = error.params as {
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

    return groups || debt ? {groups, debt} : null;
};
