import {startOfLocalDayInstant} from '../../utils/dateUtils.ts';

export type SyncAttemptFilters = {
    entityType: string;
    outcome: string;
    dateFrom: string;
    dateTo: string;
    includeUnchanged: boolean;
};

export function toQuery(values: SyncAttemptFilters): Record<string, string> {
    const query: Record<string, string> = {};
    if (values.entityType) query.entityType = values.entityType;
    if (values.outcome) query.outcome = values.outcome;
    const from = values.dateFrom ? startOfLocalDayInstant(values.dateFrom) : null;
    const to = values.dateTo ? startOfLocalDayInstant(values.dateTo, 1) : null;
    if (from) query.from = from;
    if (to) query.to = to;
    if (values.includeUnchanged) query.includeUnchanged = 'true';
    return query;
}
