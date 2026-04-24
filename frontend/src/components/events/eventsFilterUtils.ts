import type {SortDirection} from '../../api';

export type TimeWindow = 'budouci' | 'probehle' | 'vse';

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
 * Returns the ISO date string for the day before the given ISO date string.
 * Operates purely on the date components to avoid timezone edge cases.
 */
function yesterdayOf(isoDate: string): string {
    const [year, month, day] = isoDate.split('-').map(Number);
    const date = new Date(year, month - 1, day - 1);
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
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

/**
 * Returns today's date as an ISO string (YYYY-MM-DD) in local time.
 */
export function getTodayIso(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}
