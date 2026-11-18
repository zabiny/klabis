import {type ReactElement, type ReactNode} from 'react';
import {useNavigate} from 'react-router-dom';
import {Button, Modal} from '../../components/UI';
import {labels} from '../../localization';
import {formatCurrency} from '../../components/finance/financeFormatters';
import {toRoutePath} from '../../api/apiPath';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {useTemplateDialog} from '../../components/HalNavigator2/useTemplateDialog.tsx';
import {halResource} from '../../api/halResource';
import type {HalResponse} from '../../api';
import type {ApiError} from '../../api/apiError';
import {type AffectedGroup, type NegativeBalanceWarning, type SuspensionBlockedWarning} from './suspensionUtils';

interface SuspensionWarningDialogProps {
    warning: SuspensionBlockedWarning | null;
    resolvedGroupIds: ReadonlySet<string>;
    isFinishing: boolean;
    onClose: () => void;
    onGroupResolved: (groupId: string) => void;
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

const ResolvedMarker = () => (
    <span className="text-sm font-medium text-green-700">✓ {labels.suspensionWarning.resolved}</span>
);

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
    const {data, error, isLoading} = useAuthorizedQuery<HalResponse>(group.groupLink, {retry: false});
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
        content = <span className="text-sm text-text-secondary">{labels.suspensionWarning.loadingGroup}</span>;
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
        content = <span className="text-sm text-red-600">{labels.suspensionWarning.groupLoadFailed}</span>;
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

const StaticGroupRow = ({group}: Pick<RowProps, 'group'>): ReactElement => (
    <GroupRowFrame group={group}>
        <div className="flex gap-2 flex-wrap">
            <Button variant="primary" size="sm">
                {labels.suspensionWarning.designateSuccessor}
            </Button>
            <Button variant="danger" size="sm">
                {labels.suspensionWarning.dissolveGroup}
            </Button>
        </div>
    </GroupRowFrame>
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
                                            isFinishing,
                                            onClose,
                                            onGroupResolved,
                                            onFinish,
                                        }: SuspensionWarningDialogProps): ReactElement | null => {
    const navigate = useNavigate();
    if (!warning) return null;

    const groups = warning.groups ?? [];
    const {debt} = warning;
    const canFinish = !debt && groups.every(group => resolvedGroupIds.has(group.groupId));

    const handleOpenAccount = () => {
        if (!debt) return;
        onClose();
        navigate(toRoutePath(debt.accountLink));
    };

    const title = groups.length > 0 ? labels.suspensionWarning.title : labels.negativeBalanceWarning.title;

    return (
        <Modal isOpen onClose={onClose} title={title} size="lg">
            <div className="flex flex-col gap-4">
                {groups.length > 0 && (
                    <>
                        <p className="text-sm text-text-secondary">{labels.suspensionWarning.description}</p>
                        <ul className="flex flex-col gap-3">
                            {groups.map((group) => {
                                const rowProps = {
                                    group,
                                    resolved: resolvedGroupIds.has(group.groupId),
                                    onResolved: () => onGroupResolved(group.groupId),
                                };
                                if (group.groupType === 'TRAINING' || group.groupType === 'FREE') {
                                    return <ResolvableGroupRow key={group.groupId} config={RESOLVABLE_GROUPS[group.groupType]} {...rowProps}/>;
                                }
                                return <StaticGroupRow key={group.groupId} group={group}/>;
                            })}
                        </ul>
                    </>
                )}

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
