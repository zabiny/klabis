import {useSearchParams} from 'react-router-dom';
import {startOfLocalDayInstant} from '../../utils/dateUtils.ts';

export type SyncAttemptFilters = {
    entityType: string;
    outcome: string;
    dateFrom: string;
    dateTo: string;
    includeUnchanged: boolean;
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

    const handleFilterChange = (next: SyncAttemptFilters) => {
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
    };

    const extraParams: Record<string, string> = {};
    if (filters.entityType) extraParams.entityType = filters.entityType;
    if (filters.outcome) extraParams.outcome = filters.outcome;
    const from = filters.dateFrom ? startOfLocalDayInstant(filters.dateFrom) : null;
    const to = filters.dateTo ? startOfLocalDayInstant(filters.dateTo, 1) : null;
    if (from) extraParams.from = from;
    if (to) extraParams.to = to;
    if (filters.includeUnchanged) extraParams.includeUnchanged = 'true';

    return {filters, extraParams, handleFilterChange};
}
