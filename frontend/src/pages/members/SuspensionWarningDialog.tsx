import {type ReactElement} from 'react';
import {Button, Modal} from '../../components/UI';
import {labels} from '../../localization';

export interface AffectedGroup {
    groupId: string;
    groupName: string;
    groupType: string;
}

interface SuspensionWarningDialogProps {
    isOpen: boolean;
    onClose: () => void;
    affectedGroups: AffectedGroup[];
}

const groupTypeLabel = (groupType: string): string => {
    const map: Record<string, string> = {
        TRAINING: labels.suspensionWarning.groupTypeTrainingGroup,
        FAMILY: labels.suspensionWarning.groupTypeFamilyGroup,
        FREE: labels.suspensionWarning.groupTypeFreeGroup,
    };
    return map[groupType] ?? groupType;
};

export const SuspensionWarningDialog = ({isOpen, onClose, affectedGroups}: SuspensionWarningDialogProps): ReactElement | null => {
    if (!isOpen) return null;

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={labels.suspensionWarning.title} size="lg">
            <div className="flex flex-col gap-4">
                <p className="text-sm text-text-secondary">{labels.suspensionWarning.description}</p>

                <ul className="flex flex-col gap-3">
                    {affectedGroups.map((group) => (
                        <li key={group.groupId} className="border border-border rounded-md p-4 flex flex-col gap-3">
                            <div className="flex flex-col gap-1">
                                <span className="font-semibold text-text-primary">{group.groupName}</span>
                                <span className="text-xs text-text-tertiary uppercase">{groupTypeLabel(group.groupType)}</span>
                            </div>
                            <div className="flex gap-2 flex-wrap">
                                <Button variant="primary" size="sm">
                                    {labels.suspensionWarning.designateSuccessor}
                                </Button>
                                {group.groupType !== 'TRAINING' && (
                                    <Button variant="danger" size="sm">
                                        {labels.suspensionWarning.dissolveGroup}
                                    </Button>
                                )}
                            </div>
                        </li>
                    ))}
                </ul>

                <div className="flex justify-end pt-2">
                    <Button variant="secondary" onClick={onClose}>
                        {labels.buttons.cancel}
                    </Button>
                </div>
            </div>
        </Modal>
    );
};
