import { FetchError } from "../../api/authorizedFetch.ts";
import { type AffectedGroup } from "./SuspensionWarningDialog.tsx";

export const parseSuspensionWarning409 = (error: unknown): AffectedGroup[] | null => {
    if (!(error instanceof FetchError) || error.responseStatus !== 409) return null;
    try {
        const body = JSON.parse(error.responseBody ?? '{}');
        if (Array.isArray(body.affectedGroups)) return body.affectedGroups as AffectedGroup[];
    } catch {
        // not a structured 409
    }
    return null;
};
