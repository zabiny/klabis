import {type ReactElement, useCallback, useMemo, useState} from "react";
import type {EntityModel, HalFormsTemplate} from "../../api";
import {KlabisTable, TableCell} from "../../components/KlabisTable";
import {Card, Spinner} from "../../components/UI";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {useAuthorizedQuery} from "../../hooks/useAuthorizedFetch.ts";
import {usePersistedState} from "../../hooks/usePersistedState.ts";
import {useTableSort} from "../../hooks/useTableSort.ts";
import {labels} from "../../localization";
import {ArrowLeftRight, ArrowUpCircle, ArrowDownCircle, RotateCcw} from "lucide-react";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";
import {Button} from "../../components/UI";
import {formatCurrency, formatDate} from "./financeFormatters.ts";
import {HalRouteProvider} from "../../contexts/HalRouteContext.tsx";
import {MemberName} from "../../components/members/MemberName.tsx";

type TransactionItem = EntityModel<{
    id: string;
    type: string;
    amount: number;
    currency: string;
    note: string | null;
    occurredAt: string;
    recordedAt: string;
    reversesTransactionId: string | null;
    _links?: {
        self?: { href: string };
        reverses?: { href: string };
        reversedBy?: { href: string };
        recordedBy?: { href: string };
    };
    _templates?: {
        reverse?: HalFormsTemplate;
    };
}>;

export const BalanceCard = ({balance, currency}: {balance: number | undefined; currency: string | undefined}): ReactElement => {
    const isNegative = typeof balance === 'number' && balance < 0;

    return (
        <Card className="p-6">
            <div className="flex flex-col gap-2">
                <p className="text-xs uppercase font-semibold text-text-secondary tracking-wide">
                    {labels.finance.balance}
                </p>
                <p className={`text-4xl font-bold ${isNegative ? 'text-red-600 dark:text-red-400' : 'text-green-700 dark:text-green-400'}`}>
                    {formatCurrency(balance, currency)}
                </p>
            </div>
        </Card>
    );
};

const TypeCell = ({type}: {type: string}): ReactElement => {
    if (type === 'DEPOSIT') {
        return (
            <span className="flex items-center gap-1 text-green-700 dark:text-green-400 font-medium">
                <ArrowUpCircle className="w-4 h-4" />
                {labels.finance.typeDeposit}
            </span>
        );
    }
    return (
        <span className="flex items-center gap-1 text-red-600 dark:text-red-400 font-medium">
            <ArrowDownCircle className="w-4 h-4" />
            {labels.finance.typeOther}
        </span>
    );
};

const RecordedByCell = ({href}: {href: string | undefined}): ReactElement => {
    if (!href) {
        return <span>—</span>;
    }
    return (
        <HalRouteProvider routeLink={{href}}>
            <MemberName />
        </HalRouteProvider>
    );
};

export type TransactionReverseRequest = {
    reverseTarget: string;
    txId: string;
    amount: number;
    currency: string;
    occurredAt: string;
    note: string | null;
    type: string;
};

const TRANSACTIONS_TABLE_ID = 'finance-transactions';

const TransactionsTableContent = ({
    selfHref,
    extraParams,
    onReverseRequest,
}: {
    selfHref: string;
    extraParams?: Record<string, string>;
    onReverseRequest?: (tx: TransactionReverseRequest) => void;
}): ReactElement => {
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = usePersistedState('klabis-table-rows-per-page', 10);
    const defaultSort = useMemo(() => ({by: 'occurredAt', direction: 'desc' as const}), []);
    const {sort, setSort, reset} = useTableSort(TRANSACTIONS_TABLE_ID, defaultSort);

    const queryUrl = useMemo(() => {
        const url = new URL(selfHref);
        url.searchParams.set('page', String(page));
        url.searchParams.set('size', String(rowsPerPage));
        url.searchParams.delete('sort');
        if (sort) {
            url.searchParams.append('sort', `${sort.by},${sort.direction}`);
        }
        if (extraParams) {
            Object.entries(extraParams).forEach(([key, value]) => {
                url.searchParams.set(key, value);
            });
        }
        return url.toString();
    }, [selfHref, page, rowsPerPage, sort, extraParams]);

    const {data: response, error} = useAuthorizedQuery<any>(queryUrl, {
        staleTime: 30000,
        gcTime: 1000 * 60 * 5,
        retry: 1,
    });

    const tableData: TransactionItem[] = useMemo(() => {
        if (!response?._embedded?.transactions) return [];
        return response._embedded.transactions as TransactionItem[];
    }, [response]);

    const pageData = useMemo(() => response?.page, [response?.page]);

    // Build a set of transaction IDs that have been reversed (i.e. another tx on this page targets them).
    // The list endpoint does not include per-item _links.reversedBy, so we derive it from
    // reversesTransactionId fields across all visible transactions.
    const reversedIds = useMemo((): Set<string> => {
        const ids = new Set<string>();
        tableData.forEach(tx => {
            if (tx.reversesTransactionId) {
                ids.add(tx.reversesTransactionId);
            }
        });
        return ids;
    }, [tableData]);

    const renderTypeCell = useCallback(({item}: TableCellRenderProps): ReactElement => {
        const tx = item as unknown as TransactionItem;
        return <TypeCell type={tx.type as string} />;
    }, []);

    const renderAmountCell = useCallback(({item}: TableCellRenderProps): ReactElement => {
        const tx = item as unknown as TransactionItem;
        const amount = tx.amount as number;
        const currency = tx.currency as string;
        const isNegative = amount < 0;
        const isReversal = !!tx.reversesTransactionId;
        const isReversed = reversedIds.has(tx.id);

        return (
            <span className={`font-semibold ${isNegative ? 'text-red-600 dark:text-red-400' : 'text-green-700 dark:text-green-400'} ${(isReversal || isReversed) ? 'line-through opacity-60' : ''}`}>
                {formatCurrency(amount, currency)}
            </span>
        );
    }, [reversedIds]);

    const renderDateCell = useCallback(({item}: TableCellRenderProps): string => {
        return formatDate((item as unknown as TransactionItem).occurredAt as string);
    }, []);

    const renderNoteCell = useCallback(({item}: TableCellRenderProps): ReactElement => {
        const tx = item as unknown as TransactionItem;
        const isReversal = !!tx.reversesTransactionId;
        const isReversed = reversedIds.has(tx.id);

        return (
            <span className={`${(isReversal || isReversed) ? 'line-through opacity-60' : ''}`}>
                {isReversal && (
                    <span className="flex items-center gap-1 text-amber-600 dark:text-amber-400 text-xs font-medium no-underline mr-1" style={{textDecoration: 'none'}}>
                        <ArrowLeftRight className="w-3 h-3 flex-shrink-0" style={{textDecoration: 'none'}} />
                        {labels.finance.reversal}
                    </span>
                )}
                {isReversed && (
                    <span className="text-xs text-zinc-400 dark:text-zinc-500 font-medium mr-1">
                        [{labels.finance.reversed}]
                    </span>
                )}
                {(tx.note as string | null) ?? ''}
            </span>
        );
    }, [reversedIds]);

    const renderActionsCell = useCallback(({item}: TableCellRenderProps): ReactElement | null => {
        if (!onReverseRequest) return null;
        const tx = item as unknown as TransactionItem;
        const reverseTarget = tx._templates?.reverse?.target;
        if (!reverseTarget) return null;

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                <Button
                    variant="ghost"
                    size="sm"
                    aria-label={labels.finance.reverse}
                    className="text-amber-600 dark:text-amber-400"
                    onClick={(e) => {
                        e.stopPropagation();
                        onReverseRequest({
                            reverseTarget,
                            txId: tx.id,
                            amount: tx.amount as number,
                            currency: tx.currency as string,
                            occurredAt: tx.occurredAt as string,
                            note: tx.note as string | null,
                            type: tx.type as string,
                        });
                    }}
                >
                    <RotateCcw className="w-4 h-4" />
                </Button>
            </div>
        );
    }, [onReverseRequest]);

    const renderRecordedByCell = useCallback(({item}: TableCellRenderProps): ReactElement => {
        const tx = item as unknown as TransactionItem;
        const href = tx._links?.recordedBy?.href;
        return <RecordedByCell href={href} />;
    }, []);

    return (
        <KlabisTable<TransactionItem>
            data={tableData}
            page={pageData}
            error={error}
            onSortChange={(column, direction) => {
                setSort({by: column, direction});
                setPage(0);
            }}
            onSortReset={reset}
            onPageChange={setPage}
            onRowsPerPageChange={(newRowsPerPage) => {
                setRowsPerPage(newRowsPerPage);
                setPage(0);
            }}
            currentPage={page}
            rowsPerPage={rowsPerPage}
            currentSort={sort}
            defaultOrderBy="occurredAt"
            defaultOrderDirection="desc"
            emptyMessage={labels.finance.noTransactions}
        >
            <TableCell sortable column="occurredAt" dataRender={renderDateCell}>
                {labels.finance.date}
            </TableCell>
            <TableCell sortable column="type" dataRender={renderTypeCell}>
                {labels.finance.type}
            </TableCell>
            <TableCell sortable column="amount" dataRender={renderAmountCell}>
                {labels.finance.amount}
            </TableCell>
            <TableCell column="note" dataRender={renderNoteCell}>
                {labels.finance.description}
            </TableCell>
            <TableCell column="_recordedBy" dataRender={renderRecordedByCell}>
                {labels.finance.recordedBy}
            </TableCell>
            {onReverseRequest && (
                <TableCell column="_actions" dataRender={renderActionsCell}>
                    {labels.tables.actions}
                </TableCell>
            )}
        </KlabisTable>
    );
};

export const TransactionsTable = ({
    extraParams,
    onReverseRequest,
}: {
    extraParams?: Record<string, string>;
    onReverseRequest?: (tx: TransactionReverseRequest) => void;
}): ReactElement => {
    const {isLoading, route} = useHalPageData();
    const selfLink = route.getResourceLink();

    if (isLoading) {
        return (
            <div className="flex items-center gap-2 py-4">
                <Spinner />
                <span>Načítání transakcí...</span>
            </div>
        );
    }

    if (!selfLink?.href) {
        return <div className="text-sm text-text-secondary py-4">{labels.finance.noTransactions}</div>;
    }

    return <TransactionsTableContent selfHref={selfLink.href} extraParams={extraParams} onReverseRequest={onReverseRequest} />;
};
