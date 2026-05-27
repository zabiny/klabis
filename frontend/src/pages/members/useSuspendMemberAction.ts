import {useState} from "react";
import {type AffectedGroup} from "./SuspensionWarningDialog.tsx";
import {parseNegativeBalanceWarning409, parseSuspensionWarning409, type NegativeBalanceWarning} from "./suspensionUtils.ts";

export interface UseSuspendMemberActionOptions {
    /** Called when the action modal should be closed (before opening the negative-balance dialog). */
    closeActionModal: () => void;
}

export interface UseSuspendMemberActionResult {
    suspensionWarning: AffectedGroup[] | null;
    negativeBalanceWarning: NegativeBalanceWarning | null;
    clearSuspensionWarning: () => void;
    clearNegativeBalanceWarning: () => void;
    /** Compatible with HalFormDisplay onSubmitError. Returns true when the error is handled. */
    onSubmitError: (error: unknown) => true | undefined;
}

/**
 * Encapsulates 409-error handling for the suspend-member action.
 *
 * Two variants of 409 are parsed:
 * - negative-balance: closes the action modal first, then opens NegativeBalanceSuspensionDialog.
 * - affected-groups: keeps the action modal open and shows SuspensionWarningDialog alongside it.
 *
 * Unknown errors (non-409, or 409 with unexpected body) return undefined so that
 * HalFormDisplay falls back to its default error-toast handling.
 */
export const useSuspendMemberAction = ({closeActionModal}: UseSuspendMemberActionOptions): UseSuspendMemberActionResult => {
    const [suspensionWarning, setSuspensionWarning] = useState<AffectedGroup[] | null>(null);
    const [negativeBalanceWarning, setNegativeBalanceWarning] = useState<NegativeBalanceWarning | null>(null);

    const onSubmitError = (error: unknown): true | undefined => {
        const negBalance = parseNegativeBalanceWarning409(error);
        if (negBalance) {
            closeActionModal();
            setNegativeBalanceWarning(negBalance);
            return true;
        }

        const groups = parseSuspensionWarning409(error);
        if (groups) {
            setSuspensionWarning(groups);
            return true;
        }
    };

    return {
        suspensionWarning,
        negativeBalanceWarning,
        clearSuspensionWarning: () => setSuspensionWarning(null),
        clearNegativeBalanceWarning: () => setNegativeBalanceWarning(null),
        onSubmitError,
    };
};
