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

const TrainingGroupRow = ({group, resolved, onResolved}: RowProps): ReactElement => {
    const {data, error, isLoading} = useAuthorizedQuery<HalResponse>(group.groupLink, {retry: false});
    const addTrainer = halResource(data).template('addTrainer');
    const addTrainerDialog = useTemplateDialog({
        onSubmitSuccess: onResolved,
        navigateOnSuccess: false,
    });

    let content: ReactElement;
    if (resolved) {
        content = <ResolvedMarker/>;
    } else if (isLoading) {
        content = <span className="text-sm text-text-secondary">{labels.suspensionWarning.loadingGroup}</span>;
    } else if (addTrainer) {
        content = (
            <div className="flex gap-2 flex-wrap">
                <Button variant="primary" size="sm" onClick={() => addTrainerDialog.open(addTrainer)}>
                    {labels.suspensionWarning.designateSuccessor}
                </Button>
            </div>
        );
    } else if (error && (error as ApiError).status !== 403) {
        content = <span className="text-sm text-red-600">{labels.suspensionWarning.groupLoadFailed}</span>;
    } else {
        content = <span className="text-sm text-text-secondary">{labels.suspensionWarning.contactTrainingGroupAdmin}</span>;
    }

    return (
        <GroupRowFrame group={group}>
            {content}
            {addTrainerDialog.dialog}
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
                                const Row = group.groupType === 'TRAINING' ? TrainingGroupRow : StaticGroupRow;
                                return (
                                    <Row
                                        key={group.groupId}
                                        group={group}
                                        resolved={resolvedGroupIds.has(group.groupId)}
                                        onResolved={() => onGroupResolved(group.groupId)}
                                    />
                                );
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
