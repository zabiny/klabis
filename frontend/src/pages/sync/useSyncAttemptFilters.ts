import {useCallback, useMemo} from 'react';
import {useSearchParams} from 'react-router-dom';

export type SyncAttemptFilters = {
    entityType: string;
    outcome: string;
    dateFrom: string;
    dateTo: string;
    includeUnchanged: boolean;
};

const parseLocalDate = (value: string): Date => {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
};

const startOfDayInstant = (value: string): string | null => {
    const date = parseLocalDate(value);
    return isNaN(date.getTime()) ? null : date.toISOString();
};

const startOfNextDayInstant = (value: string): string | null => {
    const next = parseLocalDate(value);
    if (isNaN(next.getTime())) return null;
    next.setDate(next.getDate() + 1);
    return next.toISOString();
};

export function useSyncAttemptFilters() {
    const [searchParams, setSearchParams] = useSearchParams();

    const filters: SyncAttemptFilters = {
        entityType: searchParams.get('entityType') ?? '',
        outcome: searchParams.get('outcome') ?? '',
        dateFrom: searchParams.get('dateFrom') ?? '',
        dateTo: searchParams.get('dateTo') ?? '',
        includeUnchanged: searchParams.get('includeUnchanged') === 'true',
    };

    const handleFilterChange = useCallback((next: SyncAttemptFilters) => {
        setSearchParams((prev) => {
            const params = new URLSearchParams(prev);
            const setOrDelete = (key: string, value: string) => value ? params.set(key, value) : params.delete(key);
            setOrDelete('entityType', next.entityType);
            setOrDelete('outcome', next.outcome);
            setOrDelete('dateFrom', next.dateFrom);
            setOrDelete('dateTo', next.dateTo);
            setOrDelete('includeUnchanged', next.includeUnchanged ? 'true' : '');
            return params;
        });
    }, [setSearchParams]);

    const {entityType, outcome, dateFrom, dateTo, includeUnchanged} = filters;
    const extraParams = useMemo((): Record<string, string> => {
        const params: Record<string, string> = {};
        if (entityType) params.entityType = entityType;
        if (outcome) params.outcome = outcome;
        const from = dateFrom ? startOfDayInstant(dateFrom) : null;
        const to = dateTo ? startOfNextDayInstant(dateTo) : null;
        if (from) params.from = from;
        if (to) params.to = to;
        if (includeUnchanged) params.includeUnchanged = 'true';
        return params;
    }, [entityType, outcome, dateFrom, dateTo, includeUnchanged]);

    return {filters, extraParams, handleFilterChange};
}
