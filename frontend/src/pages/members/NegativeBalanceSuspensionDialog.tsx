import {type ReactElement} from 'react';
import {useNavigate} from 'react-router-dom';
import {Button, Modal} from '../../components/UI';
import {labels} from '../../localization';
import {formatCurrency} from '../finances/financeFormatters';
import {extractNavigationPath} from '../../utils/navigationPath';
import {type NegativeBalanceWarning} from './suspensionUtils';

interface NegativeBalanceSuspensionDialogProps {
    isOpen: boolean;
    onClose: () => void;
    warning: NegativeBalanceWarning | null;
}

export const NegativeBalanceSuspensionDialog = ({isOpen, onClose, warning}: NegativeBalanceSuspensionDialogProps): ReactElement | null => {
    const navigate = useNavigate();

    if (!isOpen || !warning) return null;

    const handleOpenAccount = () => {
        onClose();
        navigate(extractNavigationPath(warning.accountLink));
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={labels.negativeBalanceWarning.title} size="md">
            <div className="flex flex-col gap-4">
                <p className="text-sm text-text-secondary">{labels.negativeBalanceWarning.description}</p>

                <div className="rounded-md border border-border bg-surface-secondary p-4">
                    <span className="text-sm text-text-secondary">{labels.negativeBalanceWarning.currentBalance}: </span>
                    <span className="font-semibold text-red-600">
                        {formatCurrency(warning.balance.amount, warning.balance.currency)}
                    </span>
                </div>

                <div className="flex justify-end gap-3 pt-2">
                    <Button variant="secondary" onClick={onClose}>
                        {labels.negativeBalanceWarning.close}
                    </Button>
                    <Button variant="primary" onClick={handleOpenAccount}>
                        {labels.negativeBalanceWarning.openAccount}
                    </Button>
                </div>
            </div>
        </Modal>
    );
};
