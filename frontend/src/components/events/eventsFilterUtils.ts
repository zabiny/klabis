import type {SortDirection} from '../../api';
import {yesterdayOf} from '../../utils/dateUtils';

export type TimeWindow = 'budouci' | 'probehle' | 'vse';

export const REGISTERED_BY_ME = 'me' as const;

export const DEFAULT_TIME_WINDOW: TimeWindow = 'budouci';

export interface DateParams {
    dateFrom: string | undefined;
    dateTo: string | undefined;
}

export interface SortState {
    by: string;
    direction: SortDirection;
}

/**
 * Maps a time window widget value to API date query parameters.
 * Budoucí includes today (dateFrom=today).
 * Proběhlé excludes today (dateTo=yesterday).
 */
export function timeWindowToDateParams(window: TimeWindow, today: string): DateParams {
    if (window === 'budouci') {
        return {dateFrom: today, dateTo: undefined};
    }
    if (window === 'probehle') {
        return {dateFrom: undefined, dateTo: yesterdayOf(today)};
    }
    return {dateFrom: undefined, dateTo: undefined};
}

/**
 * Returns the default sort for a given time window.
 * Budoucí: ascending (nearest first); Proběhlé/Vše: descending (most recent first).
 */
export function getDefaultSortForTimeWindow(window: TimeWindow): SortState {
    return {
        by: 'eventDate',
        direction: window === 'budouci' ? 'asc' : 'desc',
    };
}

/**
 * Infers the active time window from URL date params.
 * If dateFrom is set (and dateTo is not), it's Budoucí.
 * If dateTo is set (and dateFrom is not), it's Proběhlé.
 * Otherwise it's Vše.
 */
export function getTimeWindowFromParams(
    dateFrom: string | null | undefined,
    dateTo: string | null | undefined,
): TimeWindow {
    if (dateFrom && !dateTo) return 'budouci';
    if (dateTo && !dateFrom) return 'probehle';
    return 'vse';
}

export {getTodayIso} from '../../utils/dateUtils';
