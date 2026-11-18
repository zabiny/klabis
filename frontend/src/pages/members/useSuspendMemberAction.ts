import {useMemo, useRef, useState} from "react";
import {useAuthorizedMutation} from "../../hooks/useAuthorizedFetch";
import {useToast} from "../../contexts/toastContext";
import {describeError, labels, savedMessage} from "../../localization";
import type {SubmittedRequest} from "../../components/HalNavigator2/HalFormDisplay.tsx";
import {parseSuspensionBlockedWarning409, type AffectedGroup, type SuspensionBlockedWarning} from "./suspensionUtils.ts";

export interface UseSuspendMemberActionResult {
    warning: SuspensionBlockedWarning | null;
    suspendedMemberName: string;
    resolvedGroupIds: ReadonlySet<string>;
    cascadeGroupIds: ReadonlySet<string>;
    isFinishing: boolean;
    clearWarning: () => void;
    startSuspension: (memberName: string) => void;
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
    const [blocked, setBlocked] = useState<SuspensionBlockedWarning | null>(null);
    const [pending, setPending] = useState<{request: SubmittedRequest | null; memberName: string}>({request: null, memberName: ''});
    const [resolvedGroupIds, setResolvedGroupIds] = useState<ReadonlySet<string>>(new Set());
    const [cascadeGroups, setCascadeGroups] = useState<ReadonlyMap<string, AffectedGroup>>(new Map());
    const nextMemberName = useRef('');
    const {mutate, isPending} = useAuthorizedMutation({method: 'POST'});
    const {addToast} = useToast();

    // The backend does not report cascaded groups as the guardian's own blockers, so the chosen rows
    // are kept in state and added back to the displayed warning; otherwise the admin could not undo the choice.
    const warning = useMemo(() => {
        if (!blocked) return null;
        const reported = blocked.groups ?? [];
        const carried = [...cascadeGroups.values()].filter(g => !reported.some(r => r.groupId === g.groupId));
        const groups = [...reported, ...carried];
        return {...blocked, groups: groups.length > 0 ? groups : null};
    }, [blocked, cascadeGroups]);

    const cascadeGroupIds = useMemo(() => new Set(cascadeGroups.keys()), [cascadeGroups]);

    const onSubmitError = (error: unknown, submitted?: SubmittedRequest): true | undefined => {
        const parsed = parseSuspensionBlockedWarning409(error);
        if (!parsed) return undefined;

        setBlocked(parsed);
        setPending({request: submitted ?? null, memberName: nextMemberName.current});
        setResolvedGroupIds(new Set());
        setCascadeGroups(new Map());
        return true;
    };

    const finishSuspension = () => {
        const {request} = pending;
        if (!request) return;
        const payload = cascadeGroupIds.size > 0
            ? {...request, data: {...request.data, cascadeGroupIds: [...cascadeGroupIds]}}
            : request;
        mutate(payload, {
            onSuccess: () => {
                setBlocked(null);
                addToast(savedMessage({title: labels.templates.suspendMember}), 'success');
            },
            onError: (error: unknown) => {
                const parsed = parseSuspensionBlockedWarning409(error);
                if (parsed) {
                    setBlocked(parsed);
                    setResolvedGroupIds(new Set());
                } else {
                    addToast(describeError(error), 'error');
                }
            },
        });
    };

    const chooseCascade = (groupId: string) => {
        const row = warning?.groups?.find(g => g.groupId === groupId);
        if (row) setCascadeGroups(prev => new Map(prev).set(groupId, row));
    };

    return {
        warning,
        suspendedMemberName: pending.memberName,
        resolvedGroupIds,
        cascadeGroupIds,
        isFinishing: isPending,
        clearWarning: () => setBlocked(null),
        startSuspension: (memberName) => {
            nextMemberName.current = memberName;
        },
        markGroupResolved: (groupId) => setResolvedGroupIds(prev => new Set(prev).add(groupId)),
        chooseCascade,
        cancelCascade: (groupId) => setCascadeGroups(prev => {
            const next = new Map(prev);
            next.delete(groupId);
            return next;
        }),
        finishSuspension,
        onSubmitError,
    };
};
