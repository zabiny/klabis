import {useState} from "react";
import {useAuthorizedMutation} from "../../hooks/useAuthorizedFetch";
import {useToast} from "../../contexts/toastContext";
import {describeError, labels, savedMessage} from "../../localization";
import type {SubmittedRequest} from "../../components/HalNavigator2/HalFormDisplay.tsx";
import {parseSuspensionBlockedWarning409, type SuspensionBlockedWarning} from "./suspensionUtils.ts";

export interface UseSuspendMemberActionResult {
    warning: SuspensionBlockedWarning | null;
    resolvedGroupIds: ReadonlySet<string>;
    cascadeGroupIds: ReadonlySet<string>;
    isFinishing: boolean;
    clearWarning: () => void;
    markGroupResolved: (groupId: string) => void;
    chooseCascade: (groupId: string) => void;
    cancelCascade: (groupId: string) => void;
    finishSuspension: () => void;
    /** Compatible with HalFormDisplay onSubmitError. Returns true when the error is handled. */
    onSubmitError: (error: unknown, request?: SubmittedRequest) => true | undefined;
}

/**
 * Handles the 409 of the suspend-member action: one warning carries every blocking reason
 * (debt and groups), the last request is kept so the dialog can resubmit it once the groups are resolved.
 * Returning true tells HalFormDisplay to close the suspension form.
 *
 * Unknown errors return undefined so that HalFormDisplay falls back to its default error handling.
 */
export const useSuspendMemberAction = (): UseSuspendMemberActionResult => {
    const [warning, setWarning] = useState<SuspensionBlockedWarning | null>(null);
    const [request, setRequest] = useState<SubmittedRequest | null>(null);
    const [resolvedGroupIds, setResolvedGroupIds] = useState<ReadonlySet<string>>(new Set());
    const [cascadeGroupIds, setCascadeGroupIds] = useState<ReadonlySet<string>>(new Set());
    const {mutate, isPending} = useAuthorizedMutation({method: 'POST'});
    const {addToast} = useToast();

    const applyBlocked = (blocked: SuspensionBlockedWarning, submitted: SubmittedRequest | null, keepCascade: boolean) => {
        // The backend does not report cascaded groups as the guardian's own blockers, so a blocked
        // resubmission would otherwise drop the chosen rows and the admin could no longer undo the choice.
        const reported = blocked.groups ?? [];
        const carried = keepCascade
            ? (warning?.groups ?? []).filter(g => cascadeGroupIds.has(g.groupId) && !reported.some(r => r.groupId === g.groupId))
            : [];
        const groups = [...reported, ...carried];

        setWarning({...blocked, groups: groups.length > 0 ? groups : null});
        setResolvedGroupIds(new Set());
        if (!keepCascade) setCascadeGroupIds(new Set());
        setRequest(submitted);
    };

    const onSubmitError = (error: unknown, submitted?: SubmittedRequest): true | undefined => {
        const blocked = parseSuspensionBlockedWarning409(error);
        if (!blocked) return undefined;

        applyBlocked(blocked, submitted ?? null, false);
        return true;
    };

    const finishSuspension = () => {
        if (!request) return;
        const payload = cascadeGroupIds.size > 0
            ? {...request, data: {...request.data, cascadeGroupIds: [...cascadeGroupIds]}}
            : request;
        mutate(payload, {
            onSuccess: () => {
                setWarning(null);
                addToast(savedMessage({title: labels.templates.suspendMember}), 'success');
            },
            onError: (error: unknown) => {
                const blocked = parseSuspensionBlockedWarning409(error);
                if (blocked) applyBlocked(blocked, request, true);
                else addToast(describeError(error), 'error');
            },
        });
    };

    return {
        warning,
        resolvedGroupIds,
        cascadeGroupIds,
        isFinishing: isPending,
        clearWarning: () => setWarning(null),
        markGroupResolved: (groupId) => setResolvedGroupIds(prev => new Set(prev).add(groupId)),
        chooseCascade: (groupId) => setCascadeGroupIds(prev => new Set(prev).add(groupId)),
        cancelCascade: (groupId) => setCascadeGroupIds(prev => {
            const next = new Set(prev);
            next.delete(groupId);
            return next;
        }),
        finishSuspension,
        onSubmitError,
    };
};
