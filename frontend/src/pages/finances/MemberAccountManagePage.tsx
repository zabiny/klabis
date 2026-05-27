import {type ReactElement, useCallback, useMemo, useState} from "react";
import {useSearchParams} from "react-router-dom";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {Skeleton} from "../../components/UI";
import {labels} from "../../localization";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {HalSubresourceProvider} from "../../contexts/HalRouteContext.tsx";
import {TransactionsTable, type TransactionReverseRequest, BalanceCard} from "./FinancesPage.tsx";
import {TransactionFilterBar} from "./TransactionFilterBar.tsx";
import {ReverseConfirmModal} from "./ReverseConfirmModal.tsx";
import {ArrowDownCircle, ArrowUpCircle, PiggyBank} from "lucide-react";

/**
 * Account page for finance managers viewing any member's account.
 * Renders deposit and charge buttons driven by HAL+FORMS templates —
 * buttons are hidden automatically when templates are absent in the HAL response
 * (e.g. when a member views their own account without FINANCE:MANAGE authority).
 *
 * Reverse action per transaction row is driven by the transaction's self link,
 * constructing the reverse endpoint as {selfHref}/reverse.
 */
export const MemberAccountManagePage = (): ReactElement => {
    const {isLoading, resourceData} = useHalPageData();
    const [searchParams, setSearchParams] = useSearchParams();
    const [reverseTarget, setReverseTarget] = useState<TransactionReverseRequest | null>(null);

    const urlType = searchParams.get('type') ?? '';
    const urlFrom = searchParams.get('occurredAtFrom') ?? '';
    const urlTo = searchParams.get('occurredAtTo') ?? '';

    const handleFilterChange = useCallback((next: {type: string; dateFrom: string; dateTo: string}) => {
        setSearchParams((prev) => {
            const params = new URLSearchParams(prev);
            if (next.type) { params.set('type', next.type); } else { params.delete('type'); }
            if (next.dateFrom) { params.set('occurredAtFrom', next.dateFrom); } else { params.delete('occurredAtFrom'); }
            if (next.dateTo) { params.set('occurredAtTo', next.dateTo); } else { params.delete('occurredAtTo'); }
            return params;
        });
    }, [setSearchParams]);

    const extraParams = useMemo((): Record<string, string> => {
        const params: Record<string, string> = {};
        if (urlType) params.type = urlType;
        if (urlFrom) params.occurredAtFrom = urlFrom;
        if (urlTo) params.occurredAtTo = urlTo;
        return params;
    }, [urlType, urlFrom, urlTo]);

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

    return (
        <>
            <div className="flex flex-col gap-8">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div className="flex items-center gap-3">
                        <PiggyBank className="w-8 h-8 text-primary" />
                        <h1 className="text-3xl font-bold text-text-primary">{labels.finance.memberAccountPageTitle}</h1>
                    </div>

                    {hasAnyManagerAffordance && (
                        <div className="flex flex-wrap gap-2 sm:flex-shrink-0">
                            <HalFormButton
                                name="deposit"
                                modal={true}
                                navigateOnSuccess={false}
                                icon={<ArrowUpCircle className="w-4 h-4" />}
                                variant="secondary"
                            />
                            <HalFormButton
                                name="charge"
                                modal={true}
                                navigateOnSuccess={false}
                                icon={<ArrowDownCircle className="w-4 h-4" />}
                                variant="secondary"
                            />
                        </div>
                    )}
                </div>

                <BalanceCard balance={balance} currency={currency} />

                <div className="flex flex-col gap-4">
                    <h2 className="text-xl font-bold text-text-primary">{labels.finance.transactionHistory}</h2>

                    <TransactionFilterBar
                        value={{type: urlType, dateFrom: urlFrom, dateTo: urlTo}}
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
        </>
    );
};
