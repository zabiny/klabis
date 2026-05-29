import {type ReactElement, useCallback, useState} from "react";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {useTransactionFilters} from "./useTransactionFilters.ts";
import {Button, Skeleton} from "../../components/UI";
import {labels} from "../../localization";
import {HalSubresourceProvider} from "../../contexts/HalRouteContext.tsx";
import {TransactionsTable, type TransactionReverseRequest, BalanceCard} from "./FinancesPage.tsx";
import {TransactionFilterBar} from "./TransactionFilterBar.tsx";
import {ReverseConfirmModal} from "./ReverseConfirmModal.tsx";
import {Banknote} from "lucide-react";
import {FinanceTransactionDialog} from "../../components/finance/FinanceTransactionDialog.tsx";
import type {Link} from "../../api/types.ts";
import {AccountOwnerHeader} from "./AccountOwnerHeader.tsx";

/**
 * Account page for finance managers viewing any member's account.
 * Renders deposit and charge buttons driven by HAL+FORMS templates —
 * buttons are hidden automatically when templates are absent in the HAL response
 * (e.g. when a member views their own account without FINANCE:MANAGE authority).
 *
 * Reverse action per transaction row is driven by the transaction's reverse
 * HAL affordance, which the backend exposes only on reversible transactions.
 */
export const MemberAccountManagePage = (): ReactElement => {
    const {isLoading, resourceData} = useHalPageData();
    const {filters, extraParams, handleFilterChange} = useTransactionFilters();
    const [reverseTarget, setReverseTarget] = useState<TransactionReverseRequest | null>(null);
    const [isTransactionDialogOpen, setIsTransactionDialogOpen] = useState(false);

    const handleReverseRequest = useCallback((tx: TransactionReverseRequest) => {
        setReverseTarget(tx);
    }, []);

    if (isLoading) {
        return <Skeleton />;
    }

    const balance = resourceData?.balance as number | undefined;
    const currency = resourceData?.currency as string | undefined;

    const hasDepositTemplate = !!resourceData?._templates?.deposit;
    const hasChargeTemplate = !!resourceData?._templates?.charge;
    const hasAnyManagerAffordance = hasDepositTemplate || hasChargeTemplate;

    const selfLink = resourceData?._links?.self;
    const accountLink = selfLink && !Array.isArray(selfLink) ? (selfLink as Link) : undefined;

    return (
        <>
            <div className="flex flex-col gap-8">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div className="flex items-center gap-3">
                        <Banknote className="w-8 h-8 text-primary" />
                        <h1 className="text-3xl font-bold text-text-primary">{labels.finance.memberAccountPageTitle}</h1>
                    </div>

                    {hasAnyManagerAffordance && accountLink && (
                        <div className="flex flex-wrap gap-2 sm:flex-shrink-0">
                            <Button
                                variant="secondary"
                                startIcon={<Banknote className="w-4 h-4" />}
                                aria-label={labels.finance.openTransactionDialogAriaLabel}
                                onClick={() => setIsTransactionDialogOpen(true)}
                            >
                                {labels.finance.transactionDialogTitle}
                            </Button>
                        </div>
                    )}
                </div>

                <AccountOwnerHeader />

                <BalanceCard balance={balance} currency={currency} />

                <div className="flex flex-col gap-4">
                    <h2 className="text-xl font-bold text-text-primary">{labels.finance.transactionHistory}</h2>

                    <TransactionFilterBar
                        value={filters}
                        onChange={handleFilterChange}
                    />

                    <HalSubresourceProvider subresourceLinkName="transactions">
                        <TransactionsTable
                            extraParams={extraParams}
                            onReverseRequest={hasAnyManagerAffordance ? handleReverseRequest : undefined}
                        />
                    </HalSubresourceProvider>
                </div>
            </div>

            <ReverseConfirmModal
                isOpen={reverseTarget !== null}
                transaction={reverseTarget}
                onClose={() => setReverseTarget(null)}
            />

            {accountLink && (
                <FinanceTransactionDialog
                    accountLink={accountLink}
                    isOpen={isTransactionDialogOpen}
                    onClose={() => setIsTransactionDialogOpen(false)}
                />
            )}
        </>
    );
};
