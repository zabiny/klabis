import {useState} from "react";
import {type AffectedGroup} from "./SuspensionWarningDialog.tsx";
import {type NegativeBalanceWarning, parseSuspensionBlockedWarning409} from "./suspensionUtils.ts";

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
 * The 409 body can carry a debt blocker, a group-ownership blocker, or both at once
 * (independent conditions on the backend). Both dialogs are rendered unconditionally
 * by the caller (gated on their own null state), so setting both warning states here
 * opens both dialogs simultaneously. Returning true tells HalFormDisplay to close the action dialog.
 *
 * Unknown errors (non-409, or 409 with unexpected body) return undefined so that
 * HalFormDisplay falls back to its default error-toast handling.
 */
export const useSuspendMemberAction = (): UseSuspendMemberActionResult => {
    const [suspensionWarning, setSuspensionWarning] = useState<AffectedGroup[] | null>(null);
    const [negativeBalanceWarning, setNegativeBalanceWarning] = useState<NegativeBalanceWarning | null>(null);

    const onSubmitError = (error: unknown): true | undefined => {
        const blocked = parseSuspensionBlockedWarning409(error);
        if (!blocked) return undefined;

        if (blocked.debt) {
            setNegativeBalanceWarning(blocked.debt);
        }
        if (blocked.groups) {
            setSuspensionWarning(blocked.groups);
        }
        return true;
    };

    return {
        suspensionWarning,
        negativeBalanceWarning,
        clearSuspensionWarning: () => setSuspensionWarning(null),
        clearNegativeBalanceWarning: () => setNegativeBalanceWarning(null),
        onSubmitError,
    };
};
