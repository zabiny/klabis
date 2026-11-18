import {type ReactElement, type ReactNode, useState} from 'react';
import {createPortal} from 'react-dom';
import {useNavigate} from 'react-router-dom';
import {Button, Modal} from '../../components/UI';
import {labels} from '../../localization';
import {formatPersonName} from '../../components/members/formatPersonName';
import {formatCurrency} from '../../components/finance/financeFormatters';
import {toRoutePath} from '../../api/apiPath';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {useTemplateDialog} from '../../components/HalNavigator2/useTemplateDialog.tsx';
import {halResource} from '../../api/halResource';
import type {HalResponse} from '../../api';
import type {ApiError} from '../../api/apiError';
import {type AffectedGroup, type DependentMemberWarning, type NegativeBalanceWarning, type SuspensionBlockedWarning} from './suspensionUtils';

interface SuspensionWarningDialogProps {
    warning: SuspensionBlockedWarning | null;
    resolvedGroupIds: ReadonlySet<string>;
    cascadeGroupIds: ReadonlySet<string>;
    suspendedMemberName: string;
    isFinishing: boolean;
    onClose: () => void;
    onGroupResolved: (groupId: string) => void;
    onCascadeChosen: (groupId: string) => void;
    onCascadeCancelled: (groupId: string) => void;
    onFinish: () => void;
}

const groupTypeLabel = (groupType: string): string => {
    const map: Record<string, string> = {
        TRAINING: labels.suspensionWarning.groupTypeTrainingGroup,
        LEGAL_GUARDIAN: labels.suspensionWarning.groupTypeLegalGuardianGroup,
        FREE: labels.suspensionWarning.groupTypeFreeGroup,
    };
    return map[groupType] ?? groupType;
};

interface RowProps {
    group: AffectedGroup;
    resolved: boolean;
    onResolved: () => void;
}

const GroupRowFrame = ({group, children}: {group: AffectedGroup; children: ReactNode}) => (
    <li className="border border-border rounded-md p-4 flex flex-col gap-3">
        <div className="flex flex-col gap-1">
            <span className="font-semibold text-text-primary">{group.groupName}</span>
            <span className="text-xs text-text-tertiary uppercase">{groupTypeLabel(group.groupType)}</span>
        </div>
        {children}
    </li>
);

const ResolvedMarker = ({text = labels.suspensionWarning.resolved}: {text?: string}) => (
    <span className="text-sm font-medium text-green-700">✓ {text}</span>
);

export const SuspendFormNote = () => (
    <p className="text-text-secondary">{labels.suspensionWarning.suspendFormGuardianNote}</p>
);

const useGroupResource = (group: AffectedGroup) => {
    const {data, error, isLoading} = useAuthorizedQuery<HalResponse>(group.groupLink, {retry: false});
    return {data, error, isLoading};
};

const GroupLoadStatus = ({isLoading, error}: {isLoading: boolean; error: unknown}) => {
    if (isLoading) return <span className="text-sm text-text-secondary">{labels.suspensionWarning.loadingGroup}</span>;
    if (error) return <span className="text-sm text-red-600">{labels.suspensionWarning.groupLoadFailed}</span>;
    return null;
};

interface ResolvableGroupConfig {
    successorTemplate: string;
    dissolveTemplate?: string;
    contactText: string;
    // A free group without other members cannot get a successor; its owner can only dissolve it.
    canDesignateSuccessor: (group: HalResponse | undefined) => boolean;
}

const RESOLVABLE_GROUPS: Record<'TRAINING' | 'FREE', ResolvableGroupConfig> = {
    TRAINING: {
        successorTemplate: 'addTrainer',
        contactText: labels.suspensionWarning.contactTrainingGroupAdmin,
        canDesignateSuccessor: () => true,
    },
    FREE: {
        successorTemplate: 'addGroupOwner',
        dissolveTemplate: 'deleteGroup',
        contactText: labels.suspensionWarning.contactFreeGroupAdmin,
        canDesignateSuccessor: (group) => ((group as {members?: unknown[]} | undefined)?.members?.length ?? 0) > 0,
    },
};

const ResolvableGroupRow = ({group, resolved, onResolved, config}: RowProps & {config: ResolvableGroupConfig}): ReactElement => {
    const {data, error, isLoading} = useGroupResource(group);
    const groupActions = halResource(data);
    const successor = config.canDesignateSuccessor(data) ? groupActions.template(config.successorTemplate) : undefined;
    const dissolve = config.dissolveTemplate ? groupActions.template(config.dissolveTemplate) : undefined;
    const successorDialog = useTemplateDialog({onSubmitSuccess: onResolved, navigateOnSuccess: false});
    const dissolveDialog = useTemplateDialog({
        onSubmitSuccess: onResolved,
        navigateOnSuccess: false,
        note: labels.suspensionWarning.dissolveConfirmation(group.groupName),
    });

    let content: ReactElement;
    if (resolved) {
        content = <ResolvedMarker/>;
    } else if (isLoading) {
        content = <GroupLoadStatus isLoading error={null}/>;
    } else if (successor || dissolve) {
        content = (
            <div className="flex gap-2 flex-wrap">
                {successor && (
                    <Button variant="primary" size="sm" onClick={() => successorDialog.open(successor)}>
                        {labels.suspensionWarning.designateSuccessor}
                    </Button>
                )}
                {dissolve && (
                    <Button variant="danger" size="sm" onClick={() => dissolveDialog.open(dissolve)}>
                        {labels.suspensionWarning.dissolveGroup}
                    </Button>
                )}
            </div>
        );
    } else if (error && (error as ApiError).status !== 403) {
        content = <GroupLoadStatus isLoading={false} error={error}/>;
    } else {
        content = <span className="text-sm text-text-secondary">{config.contactText}</span>;
    }

    return (
        <GroupRowFrame group={group}>
            {content}
            {successorDialog.dialog}
            {dissolveDialog.dialog}
        </GroupRowFrame>
    );
};

interface LegalGuardianGroupData {
    minors?: Array<{firstName?: string; lastName?: string}>;
}

interface LegalGuardianRowProps {
    group: AffectedGroup;
    cascaded: boolean;
    guardianName: string;
    onChosen: () => void;
    onCancelled: () => void;
    onSuccessorRequested: () => void;
}

const LegalGuardianGroupRow = ({group, cascaded, guardianName, onChosen, onCancelled, onSuccessorRequested}: LegalGuardianRowProps): ReactElement => {
    const {data, error, isLoading} = useGroupResource(group);
    const [confirming, setConfirming] = useState(false);
    const groupData = data as LegalGuardianGroupData | undefined;

    const groupUnavailable = isLoading || !!error;

    let content: ReactElement;
    if (cascaded) {
        content = (
            <div className="flex items-center gap-3 flex-wrap">
                <ResolvedMarker text={labels.suspensionWarning.suspendWholeGroupMarked}/>
                <Button variant="secondary" size="sm" onClick={onCancelled}>
                    {labels.suspensionWarning.suspendWholeGroupCancel}
                </Button>
            </div>
        );
    } else {
        content = (
            <div className="flex flex-col gap-2">
                <div className="flex gap-2 flex-wrap">
                    <Button variant="primary" size="sm" onClick={onSuccessorRequested}>
                        {labels.suspensionWarning.designateSuccessor}
                    </Button>
                    <Button variant="danger" size="sm" disabled={groupUnavailable} onClick={() => setConfirming(true)}>
                        {labels.suspensionWarning.suspendWholeGroup}
                    </Button>
                </div>
                <GroupLoadStatus isLoading={isLoading} error={error}/>
            </div>
        );
    }

    return (
        <GroupRowFrame group={group}>
            {content}
            {confirming && createPortal(
                <Modal isOpen onClose={() => setConfirming(false)} title={labels.suspensionWarning.suspendWholeGroupConfirmTitle} size="md">
                    <div className="flex flex-col gap-3">
                        <p className="text-sm text-text-secondary">{labels.suspensionWarning.suspendWholeGroupConfirmText}</p>
                        <div>
                            <span className="text-xs text-text-tertiary uppercase">{labels.suspensionWarning.suspendWholeGroupGuardian}</span>
                            <p className="text-text-primary">{guardianName}</p>
                        </div>
                        <div>
                            <span className="text-xs text-text-tertiary uppercase">{labels.suspensionWarning.suspendWholeGroupMinors}</span>
                            <ul className="list-disc pl-5 text-text-primary">
                                {(groupData?.minors ?? []).map((minor, index) => (
                                    <li key={index}>{formatPersonName(minor)}</li>
                                ))}
                            </ul>
                        </div>
                        <div className="flex justify-end gap-3 pt-2">
                            <Button variant="secondary" onClick={() => setConfirming(false)}>
                                {labels.buttons.cancel}
                            </Button>
                            <Button variant="danger" onClick={() => {
                                setConfirming(false);
                                onChosen();
                            }}>
                                {labels.suspensionWarning.suspendWholeGroupConfirm}
                            </Button>
                        </div>
                    </div>
                </Modal>,
                document.body,
            )}
        </GroupRowFrame>
    );
};

const DependentMembersSection = ({dependents}: {dependents: DependentMemberWarning[]}) => (
    <div className="flex flex-col gap-3">
        <h3 className="font-semibold text-text-primary">{labels.suspensionWarning.dependentMembersTitle}</h3>
        <p className="text-sm text-text-secondary">{labels.suspensionWarning.dependentMembersDescription}</p>
        <ul className="flex flex-col gap-2">
            {dependents.map((dependent) => (
                <li key={dependent.memberId} className="border border-border rounded-md p-3 flex flex-col gap-1">
                    <span className="font-semibold text-text-primary">{dependent.memberName}</span>
                    {dependent.debt && (
                        <span className="text-sm text-red-600">
                            {labels.suspensionWarning.dependentDebt}: {formatCurrency(dependent.debt.balance.amount, dependent.debt.balance.currency)}
                        </span>
                    )}
                    {(dependent.groups ?? []).map((group) => (
                        <span key={group.groupId} className="text-sm text-text-secondary">
                            {labels.suspensionWarning.dependentLastOwner}: {group.groupName}
                        </span>
                    ))}
                </li>
            ))}
        </ul>
    </div>
);

const DebtSection = ({debt, onOpenAccount}: {debt: NegativeBalanceWarning; onOpenAccount: () => void}) => (
    <div className="flex flex-col gap-3">
        <p className="text-sm text-text-secondary">{labels.negativeBalanceWarning.description}</p>
        <div className="rounded-md border border-border bg-surface-secondary p-4 flex items-center justify-between gap-3">
            <div>
                <span className="text-sm text-text-secondary">{labels.negativeBalanceWarning.currentBalance}: </span>
                <span className="font-semibold text-red-600">
                    {formatCurrency(debt.balance.amount, debt.balance.currency)}
                </span>
            </div>
            <Button variant="secondary" size="sm" onClick={onOpenAccount}>
                {labels.negativeBalanceWarning.openAccount}
            </Button>
        </div>
    </div>
);

export const SuspensionWarningDialog = ({
                                            warning,
                                            resolvedGroupIds,
                                            cascadeGroupIds,
                                            suspendedMemberName,
                                            isFinishing,
                                            onClose,
                                            onGroupResolved,
                                            onCascadeChosen,
                                            onCascadeCancelled,
                                            onFinish,
                                        }: SuspensionWarningDialogProps): ReactElement | null => {
    const navigate = useNavigate();
    if (!warning) return null;

    const groups = warning.groups ?? [];
    const {debt} = warning;
    // Dependents' blockers belong to the cascade; once it is cancelled they no longer apply.
    const dependents = cascadeGroupIds.size > 0 ? warning.dependentMembers ?? [] : [];
    const isResolved = (groupId: string) => resolvedGroupIds.has(groupId) || cascadeGroupIds.has(groupId);
    const canFinish = !debt && dependents.length === 0 && groups.every(group => isResolved(group.groupId));

    const handleOpenAccount = () => {
        if (!debt) return;
        onClose();
        navigate(toRoutePath(debt.accountLink));
    };

    const title = groups.length > 0 || dependents.length > 0 ? labels.suspensionWarning.title : labels.negativeBalanceWarning.title;

    return (
        <Modal isOpen onClose={onClose} title={title} size="lg">
            <div className="flex flex-col gap-4">
                {groups.length > 0 && (
                    <>
                        <p className="text-sm text-text-secondary">{labels.suspensionWarning.description}</p>
                        <ul className="flex flex-col gap-3">
                            {groups.map((group) => {
                                if (group.groupType === 'LEGAL_GUARDIAN') {
                                    return (
                                        <LegalGuardianGroupRow
                                            key={group.groupId}
                                            group={group}
                                            cascaded={cascadeGroupIds.has(group.groupId)}
                                            guardianName={suspendedMemberName}
                                            onChosen={() => onCascadeChosen(group.groupId)}
                                            onCancelled={() => onCascadeCancelled(group.groupId)}
                                            onSuccessorRequested={() => {
                                                onClose();
                                                navigate(toRoutePath(group.groupLink));
                                            }}
                                        />
                                    );
                                }
                                const rowProps = {
                                    group,
                                    resolved: resolvedGroupIds.has(group.groupId),
                                    onResolved: () => onGroupResolved(group.groupId),
                                };
                                return <ResolvableGroupRow key={group.groupId} config={RESOLVABLE_GROUPS[group.groupType]} {...rowProps}/>;
                            })}
                        </ul>
                    </>
                )}

                {dependents.length > 0 && <DependentMembersSection dependents={dependents}/>}

                {debt && <DebtSection debt={debt} onOpenAccount={handleOpenAccount}/>}

                <div className="flex justify-end gap-3 pt-2">
                    <Button variant="secondary" onClick={onClose}>
                        {labels.buttons.cancel}
                    </Button>
                    {canFinish && (
                        <Button variant="primary" onClick={onFinish} disabled={isFinishing}>
                            {labels.suspensionWarning.finishSuspension}
                        </Button>
                    )}
                </div>
            </div>
        </Modal>
    );
};
