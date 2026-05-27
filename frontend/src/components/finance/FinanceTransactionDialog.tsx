import {type ReactElement, useEffect, useState} from 'react';
import {useQuery, useQueryClient} from '@tanstack/react-query';
import {Banknote, Check, CircleArrowDown, CircleArrowUp, X} from 'lucide-react';
import {labels} from '../../localization';
import {Button} from '../UI';
import {authorizedFetch} from '../../api/authorizedFetch';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import type {HalResponse, HalFormsTemplate, Link} from '../../api/types';
import {formatCurrency} from '../../pages/finances/financeFormatters';

export interface FinanceTransactionDialogProps {
    accountLink: Link;
    isOpen: boolean;
    onClose: () => void;
}

type TabId = 'deposit' | 'charge';

const LAST_TAB_KEY = 'klabis.financeDialog.lastTab';

interface AccountData extends HalResponse {
    balance?: number;
    currency?: string;
}

interface MemberData {
    firstName?: string;
    lastName?: string;
    registrationNumber?: string;
}

function resolveInitialTab(
    storedTab: string | null,
    hasDeposit: boolean,
    hasCharge: boolean
): TabId {
    if (storedTab === 'charge' && hasCharge) return 'charge';
    if (storedTab === 'deposit' && hasDeposit) return 'deposit';
    return hasDeposit ? 'deposit' : 'charge';
}

function TabButton({
    tabId,
    activeTab,
    label,
    icon,
    onClick,
}: {
    tabId: TabId;
    activeTab: TabId;
    label: string;
    icon: ReactElement;
    onClick: (tab: TabId) => void;
}) {
    const isActive = activeTab === tabId;
    return (
        <button
            role="tab"
            aria-selected={isActive}
            onClick={() => onClick(tabId)}
            className={`flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors ${
                isActive
                    ? 'text-primary border-b-2 border-primary'
                    : 'text-text-secondary hover:text-text-primary'
            }`}
        >
            {icon}
            {label}
        </button>
    );
}

export const FinanceTransactionDialog = ({
    accountLink,
    isOpen,
    onClose,
}: FinanceTransactionDialogProps): ReactElement | null => {
    const queryClient = useQueryClient();

    const [activeTab, setActiveTab] = useState<TabId>('deposit');
    const [amount, setAmount] = useState('');
    const [note, setNote] = useState('');
    const [occurredAt, setOccurredAt] = useState('');
    const [submitError, setSubmitError] = useState<string | null>(null);

    const {data: accountData, isLoading: isAccountLoading} = useQuery<AccountData>({
        queryKey: ['member-account', accountLink.href],
        queryFn: async () => {
            // accountLink.href is guaranteed by the parent passing a valid account self-link
            const response = await authorizedFetch(accountLink.href!);
            return response.json();
        },
        enabled: isOpen,
        staleTime: 0,
        gcTime: 0,
        retry: false,
    });

    const accountOwnerLink = accountData?._links?.accountOwner;
    const accountOwnerHref =
        accountOwnerLink && !Array.isArray(accountOwnerLink)
            ? (accountOwnerLink as Link).href
            : undefined;

    const {data: memberData, isLoading: isMemberLoading} = useQuery<MemberData>({
        queryKey: ['member', accountOwnerHref ?? ''],
        queryFn: async () => {
            const response = await authorizedFetch(accountOwnerHref!);
            return response.json();
        },
        enabled: isOpen && !!accountOwnerHref,
        staleTime: 0,
        gcTime: 0,
        retry: false,
    });

    const depositTemplate = accountData?._templates?.deposit as HalFormsTemplate | undefined;
    const chargeTemplate = accountData?._templates?.charge as HalFormsTemplate | undefined;
    const hasDeposit = !!depositTemplate;
    const hasCharge = !!chargeTemplate;
    const hasBothTemplates = hasDeposit && hasCharge;
    const hasAnyTemplate = hasDeposit || hasCharge;

    useEffect(() => {
        if (isOpen && hasAnyTemplate) {
            const stored = localStorage.getItem(LAST_TAB_KEY);
            setActiveTab(resolveInitialTab(stored, hasDeposit, hasCharge));
        }
    }, [isOpen, hasDeposit, hasCharge, hasAnyTemplate]);

    const {mutate: submitTransaction, isPending} = useAuthorizedMutation({method: 'POST'});

    if (!isOpen) return null;

    const isLoading = isAccountLoading || (!!accountOwnerHref && isMemberLoading);

    const handleTabChange = (tab: TabId) => {
        setActiveTab(tab);
        localStorage.setItem(LAST_TAB_KEY, tab);
    };

    const handleClose = () => {
        setAmount('');
        setNote('');
        setOccurredAt('');
        setSubmitError(null);
        onClose();
    };

    const handleSubmit = () => {
        const template = activeTab === 'deposit' ? depositTemplate : chargeTemplate;
        if (!template?.target) return;

        setSubmitError(null);
        const data: Record<string, unknown> = {amount: parseFloat(amount)};
        if (occurredAt) data.occurredAt = occurredAt;
        if (note.trim()) data.note = note.trim();

        submitTransaction(
            {url: template.target, data},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['member-account', accountLink.href]});
                    queryClient.invalidateQueries({queryKey: ['transactions']});
                    handleClose();
                },
                onError: (error: unknown) => {
                    setSubmitError(error instanceof Error ? error.message : labels.errors.requestFailed);
                },
            }
        );
    };

    const isSubmitDisabled = !amount || parseFloat(amount) <= 0 || isPending || !hasAnyTemplate;

    const memberFullName = memberData
        ? `${memberData.firstName ?? ''} ${memberData.lastName ?? ''}`.trim()
        : '';

    return (
        <>
            <div
                className="fixed inset-0 z-40 bg-black bg-opacity-60 transition-opacity duration-base"
                onClick={handleClose}
                aria-hidden="true"
                data-testid="modal-backdrop"
            />
            <div
                className="fixed inset-0 z-50 flex items-center justify-center p-4"
                role="dialog"
                aria-modal="true"
                aria-labelledby="finance-dialog-title"
            >
                <div
                    className="bg-surface-raised rounded-md shadow-lg w-full max-w-md animate-scale-in"
                    onClick={(e) => e.stopPropagation()}
                >
                    {isLoading ? (
                        <DialogSkeleton />
                    ) : (
                        <>
                            <DialogHeader
                                memberName={memberFullName}
                                registrationNumber={memberData?.registrationNumber}
                                onClose={handleClose}
                            />
                            <BalanceStrip
                                balance={accountData?.balance}
                                currency={accountData?.currency}
                            />
                            {hasBothTemplates && (
                                <div role="tablist" className="flex border-b border-border px-6">
                                    <TabButton
                                        tabId="deposit"
                                        activeTab={activeTab}
                                        label={labels.finance.tabDeposit}
                                        icon={<CircleArrowDown className="w-4 h-4" />}
                                        onClick={handleTabChange}
                                    />
                                    <TabButton
                                        tabId="charge"
                                        activeTab={activeTab}
                                        label={labels.finance.tabCharge}
                                        icon={<CircleArrowUp className="w-4 h-4" />}
                                        onClick={handleTabChange}
                                    />
                                </div>
                            )}
                            <div className="px-6 py-4">
                                {hasAnyTemplate ? (
                                    <TransactionForm
                                        amount={amount}
                                        note={note}
                                        occurredAt={occurredAt}
                                        onAmountChange={setAmount}
                                        onNoteChange={setNote}
                                        onOccurredAtChange={setOccurredAt}
                                        submitError={submitError}
                                    />
                                ) : (
                                    <p className="text-sm text-text-secondary">
                                        Žádná operace není povolena
                                    </p>
                                )}
                            </div>
                            <div className="border-t border-border px-6 py-4 flex justify-between gap-3">
                                <Button variant="secondary" onClick={handleClose} disabled={isPending}>
                                    {labels.buttons.cancel}
                                </Button>
                                {hasAnyTemplate && (
                                    <CtaButton
                                        activeTab={activeTab}
                                        disabled={isSubmitDisabled}
                                        onClick={handleSubmit}
                                    />
                                )}
                            </div>
                        </>
                    )}
                </div>
            </div>
        </>
    );
};

function DialogSkeleton() {
    return (
        <div data-testid="finance-dialog-skeleton" className="px-6 py-8 flex flex-col gap-4">
            <div className="h-6 bg-surface-raised rounded animate-pulse w-2/3" />
            <div className="h-4 bg-surface-raised rounded animate-pulse w-1/2" />
            <div className="h-10 bg-surface-raised rounded animate-pulse" />
            <div className="h-10 bg-surface-raised rounded animate-pulse" />
        </div>
    );
}

function DialogHeader({
    memberName,
    registrationNumber,
    onClose,
}: {
    memberName: string;
    registrationNumber?: string;
    onClose: () => void;
}) {
    return (
        <div
            className="flex items-center justify-between border-b border-border px-6 py-4 bg-surface-base rounded-t-md"
            data-testid="modal-header"
        >
            <div className="flex items-center gap-3">
                <Banknote className="w-5 h-5 text-primary" />
                <div className="flex flex-col">
                    <h2
                        id="finance-dialog-title"
                        className="text-lg font-semibold text-text-primary font-display"
                    >
                        {labels.finance.transactionDialogTitle}
                    </h2>
                    {memberName && (
                        <p className="text-sm text-text-secondary">
                            {memberName}
                            {registrationNumber && <> · {registrationNumber}</>}
                        </p>
                    )}
                </div>
            </div>
            <button
                onClick={onClose}
                className="ml-auto text-text-secondary hover:text-text-primary transition-colors"
                aria-label="Close modal"
            >
                <X className="w-6 h-6" />
            </button>
        </div>
    );
}

function BalanceStrip({balance, currency}: {balance?: number; currency?: string}) {
    return (
        <div className="px-6 py-3 bg-surface-raised border-b border-border">
            <div className="flex items-center justify-between">
                <span className="text-sm text-text-secondary">{labels.finance.balance}</span>
                <span className="text-lg font-semibold text-text-primary">
                    {formatCurrency(balance, currency)}
                </span>
            </div>
        </div>
    );
}

function TransactionForm({
    amount,
    note,
    occurredAt,
    onAmountChange,
    onNoteChange,
    onOccurredAtChange,
    submitError,
}: {
    amount: string;
    note: string;
    occurredAt: string;
    onAmountChange: (v: string) => void;
    onNoteChange: (v: string) => void;
    onOccurredAtChange: (v: string) => void;
    submitError: string | null;
}) {
    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
                <label htmlFor="finance-amount" className="text-sm font-medium text-text-primary">
                    Částka *
                </label>
                <input
                    id="finance-amount"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={amount}
                    onChange={(e) => onAmountChange(e.target.value)}
                    placeholder="0,00"
                    className="px-3 py-2 rounded border border-border bg-surface text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
            </div>
            <div className="flex flex-col gap-1">
                <label htmlFor="finance-occurredAt" className="text-sm font-medium text-text-primary">
                    {labels.finance.date}
                </label>
                <input
                    id="finance-occurredAt"
                    type="date"
                    value={occurredAt}
                    onChange={(e) => onOccurredAtChange(e.target.value)}
                    className="px-3 py-2 rounded border border-border bg-surface text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
            </div>
            <div className="flex flex-col gap-1">
                <label htmlFor="finance-note" className="text-sm font-medium text-text-primary">
                    Poznámka
                </label>
                <input
                    id="finance-note"
                    type="text"
                    value={note}
                    onChange={(e) => onNoteChange(e.target.value)}
                    placeholder="Volitelná poznámka..."
                    className="px-3 py-2 rounded border border-border bg-surface text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
            </div>
            {submitError && (
                <div className="p-3 rounded-md bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
                    <p className="text-sm text-red-700 dark:text-red-300">{submitError}</p>
                </div>
            )}
        </div>
    );
}

function CtaButton({
    activeTab,
    disabled,
    onClick,
}: {
    activeTab: TabId;
    disabled: boolean;
    onClick: () => void;
}) {
    const isDeposit = activeTab === 'deposit';
    const label = isDeposit ? labels.finance.tabDeposit : labels.finance.tabCharge;
    const colorClasses = isDeposit
        ? 'bg-emerald-600 hover:bg-emerald-700'
        : 'bg-red-600 hover:bg-red-700';

    return (
        <button
            className={`inline-flex items-center justify-center gap-2 font-medium rounded-md transition-all duration-fast focus:outline-none focus:ring-2 focus:ring-accent focus:ring-offset-0 text-white shadow-sm disabled:opacity-50 disabled:cursor-not-allowed px-4 py-2.5 text-base ${colorClasses}`}
            disabled={disabled}
            onClick={onClick}
        >
            <Check className="w-4 h-4" />
            {label}
        </button>
    );
}
