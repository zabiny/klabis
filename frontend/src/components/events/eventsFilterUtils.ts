import type {SortDirection} from '../../api';
import {yesterdayOf} from '../../utils/dateUtils';

export type TimeWindow = 'budouci' | 'probehle' | 'vse';

export const REGISTERED_BY_ME = 'me' as const;

export const DEFAULT_TIME_WINDOW: TimeWindow = 'budouci';

const VALID_TIME_WINDOWS: TimeWindow[] = ['budouci', 'probehle', 'vse'];

/**
 * Maps the ?when= URL param value to a TimeWindow.
 * Returns DEFAULT_TIME_WINDOW for unknown or missing values.
 */
export function getTimeWindowFromWhenParam(when: string | null | undefined): TimeWindow {
    if (when && VALID_TIME_WINDOWS.includes(when as TimeWindow)) {
        return when as TimeWindow;
    }
    return DEFAULT_TIME_WINDOW;
}

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

export interface YearDateParams {
    dateFrom: string;
    dateTo: string;
}

export function yearToDateParams(year: number): YearDateParams {
    return {dateFrom: `${year}-01-01`, dateTo: `${year}-12-31`};
}

/**
 * Combines year and time-window filters with AND semantics to produce API dateFrom/dateTo params.
 *
 * When year=Y and when=budouci: dateFrom=max(today, Y-01-01), dateTo=Y-12-31
 * When year=Y and when=probehle: dateFrom=Y-01-01, dateTo=min(yesterday, Y-12-31)
 * When year=Y and when=vse: dateFrom=Y-01-01, dateTo=Y-12-31
 * When year=null: delegates entirely to time-window (no year constraint)
 */
export function combineYearAndTimeWindowToDateParams(
    year: number | null,
    timeWindow: TimeWindow,
    today: string,
): DateParams {
    if (year === null) {
        return timeWindowToDateParams(timeWindow, today);
    }

    const yearStart = `${year}-01-01`;
    const yearEnd = `${year}-12-31`;

    if (timeWindow === 'vse') {
        return {dateFrom: yearStart, dateTo: yearEnd};
    }

    if (timeWindow === 'budouci') {
        const dateFrom = today > yearStart ? today : yearStart;
        return {dateFrom, dateTo: yearEnd};
    }

    // probehle: dateFrom=Y-01-01, dateTo=min(yesterday, Y-12-31)
    const yesterday = yesterdayOf(today);
    const dateTo = yesterday < yearEnd ? yesterday : yearEnd;
    return {dateFrom: yearStart, dateTo};
}

export function getYearFromParams(
    dateFrom: string | null | undefined,
    dateTo: string | null | undefined,
): number | null {
    if (!dateFrom || !dateTo) return null;
    if (!dateFrom.endsWith('-01-01') || !dateTo.endsWith('-12-31')) return null;
    const fromYear = parseInt(dateFrom.slice(0, 4), 10);
    const toYear = parseInt(dateTo.slice(0, 4), 10);
    if (Number.isNaN(fromYear) || Number.isNaN(toYear)) return null;
    if (fromYear !== toYear) return null;
    return fromYear;
}

export function isCurrentYear(year: number | null): boolean {
    if (year === null) return false;
    return year === new Date().getFullYear();
}

export function getYearRange(): number[] {
    const current = new Date().getFullYear();
    const years: number[] = [];
    for (let y = current - 10; y <= current + 2; y++) {
        years.push(y);
    }
    return years;
}
