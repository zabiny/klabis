import {useCallback, useMemo} from "react";
import {useSearchParams} from "react-router-dom";

export type TransactionFilters = {
    type: string;
    dateFrom: string;
    dateTo: string;
};

export type UseTransactionFiltersResult = {
    filters: TransactionFilters;
    extraParams: Record<string, string>;
    handleFilterChange: (next: TransactionFilters) => void;
};

export function useTransactionFilters(): UseTransactionFiltersResult {
    const [searchParams, setSearchParams] = useSearchParams();

    const urlType = searchParams.get('type') ?? '';
    const urlFrom = searchParams.get('occurredAtFrom') ?? '';
    const urlTo = searchParams.get('occurredAtTo') ?? '';

    const handleFilterChange = useCallback((next: TransactionFilters) => {
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

    return {
        filters: {type: urlType, dateFrom: urlFrom, dateTo: urlTo},
        extraParams,
        handleFilterChange,
    };
}
