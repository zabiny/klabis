import {FetchError} from "../../api/authorizedFetch.ts";
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
    if (!(error instanceof FetchError) || error.responseStatus !== 409) return null;
    try {
        const body = JSON.parse(error.responseBody ?? '{}');

        const groups = Array.isArray(body.groups?.affectedGroups)
            ? (body.groups.affectedGroups as AffectedGroup[])
            : null;

        const debt =
            body.debt && typeof body.debt.balance?.amount === 'number' && typeof body.debt.accountLink === 'string'
                ? (body.debt as NegativeBalanceWarning)
                : null;

        if (groups || debt) return {groups, debt};
    } catch {
        // not a structured 409
    }
    return null;
};
