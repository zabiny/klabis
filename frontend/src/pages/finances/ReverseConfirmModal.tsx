import {type ReactElement, useState} from 'react';
import {Button, Modal} from '../../components/UI';
import {labels} from '../../localization';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch.ts';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation.ts';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {useToast} from '../../contexts/ToastContext.tsx';
import type {TransactionReverseRequest} from './FinancesPage.tsx';
import {toFormValidationError} from '../../api/hateoas.ts';
import {formatCurrency, formatDate} from './financeFormatters.ts';

interface ReverseConfirmModalProps {
    isOpen: boolean;
    transaction: TransactionReverseRequest | null;
    onClose: () => void;
}

function toPathname(href: string): string {
    try {
        return new URL(href).pathname;
    } catch {
        return href;
    }
}

/**
 * Confirmation modal for reversing a finance transaction.
 * POSTs to the transaction's `reverse` HAL affordance target with optional note and occurredAt.
 * Bypasses the overdraft limit by design (backend rule).
 */
export const ReverseConfirmModal = ({isOpen, transaction, onClose}: ReverseConfirmModalProps): ReactElement | null => {
    const [note, setNote] = useState('');
    const [occurredAt, setOccurredAt] = useState('');
    const [submitError, setSubmitError] = useState<string | null>(null);

    const {route} = useHalPageData();
    const {invalidateAllCaches} = useFormCacheInvalidation();
    const {addToast} = useToast();

    const {mutate: submitReverse, isPending} = useAuthorizedMutation({method: 'POST'});

    const handleClose = () => {
        setNote('');
        setOccurredAt('');
        setSubmitError(null);
        onClose();
    };

    const handleConfirm = () => {
        if (!transaction) return;
        setSubmitError(null);

        const url = toPathname(transaction.reverseTarget);
        const data: Record<string, unknown> = {};
        if (note.trim()) data.note = note.trim();
        if (occurredAt) data.occurredAt = occurredAt;

        submitReverse({url, data}, {
            onSuccess: async () => {
                await invalidateAllCaches();
                await route.refetch();
                addToast(labels.finance.reversal + ' — úspěšně uloženo', 'success');
                handleClose();
            },
            onError: (error: unknown) => {
                const formError = toFormValidationError(error);
                setSubmitError(formError.message);
            },
        });
    };

    if (!isOpen || !transaction) return null;

    const txTypeLabel = transaction.type === 'DEPOSIT' ? labels.finance.typeDeposit : labels.finance.typeOther;

    return (
        <Modal
            isOpen={isOpen}
            onClose={handleClose}
            title={labels.finance.reverseConfirmTitle}
            size="md"
        >
            <div className="flex flex-col gap-6">
                <div className="p-4 rounded-md bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
                    <p className="text-sm text-amber-800 dark:text-amber-200">
                        {labels.finance.reverseConfirmDescription}
                    </p>
                </div>

                <div className="flex flex-col gap-2">
                    <div className="flex justify-between text-sm">
                        <span className="text-text-secondary">{labels.finance.type}:</span>
                        <span className="font-medium">{txTypeLabel}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                        <span className="text-text-secondary">{labels.finance.amount}:</span>
                        <span className="font-medium">{formatCurrency(transaction.amount, transaction.currency, {absolute: true})}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                        <span className="text-text-secondary">{labels.finance.date}:</span>
                        <span className="font-medium">{formatDate(transaction.occurredAt)}</span>
                    </div>
                    {transaction.note && (
                        <div className="flex justify-between text-sm">
                            <span className="text-text-secondary">{labels.finance.description}:</span>
                            <span className="font-medium">{transaction.note}</span>
                        </div>
                    )}
                </div>

                <div className="flex flex-col gap-4">
                    <div className="flex flex-col gap-1">
                        <label className="text-sm font-medium text-text-primary" htmlFor="reverse-note">
                            {labels.finance.description} (nepovinné)
                        </label>
                        <input
                            id="reverse-note"
                            type="text"
                            value={note}
                            onChange={(e) => setNote(e.target.value)}
                            placeholder="Důvod storna..."
                            className="px-3 py-2 rounded border border-border bg-surface text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>

                    <div className="flex flex-col gap-1">
                        <label className="text-sm font-medium text-text-primary" htmlFor="reverse-date">
                            {labels.finance.date} (nepovinné)
                        </label>
                        <input
                            id="reverse-date"
                            type="date"
                            value={occurredAt}
                            onChange={(e) => setOccurredAt(e.target.value)}
                            className="px-3 py-2 rounded border border-border bg-surface text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                </div>

                {submitError && (
                    <div className="p-3 rounded-md bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
                        <p className="text-sm text-red-700 dark:text-red-300">{submitError}</p>
                    </div>
                )}

                <div className="flex justify-end gap-3 pt-2 border-t border-border">
                    <Button variant="secondary" onClick={handleClose} disabled={isPending}>
                        {labels.finance.cancelReverse}
                    </Button>
                    <Button variant="danger" onClick={handleConfirm} disabled={isPending}>
                        {isPending ? labels.buttons.submitting : labels.finance.confirmReverse}
                    </Button>
                </div>
            </div>
        </Modal>
    );
};
