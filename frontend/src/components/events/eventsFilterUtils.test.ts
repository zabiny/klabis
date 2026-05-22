import {describe, expect, it} from 'vitest';
import {
    DEFAULT_TIME_WINDOW,
    getDefaultSortForTimeWindow,
    getTimeWindowFromParams,
    getYearFromParams,
    getYearRange,
    timeWindowToDateParams,
    type TimeWindow,
    yearToDateParams,
} from './eventsFilterUtils';

describe('eventsFilterUtils', () => {
    describe('timeWindowToDateParams', () => {
        it('maps Budoucí to dateFrom=today', () => {
            const today = '2024-06-15';
            const result = timeWindowToDateParams('budouci', today);
            expect(result).toEqual({dateFrom: '2024-06-15', dateTo: undefined});
        });

        it('maps Proběhlé to dateTo=today-1', () => {
            const today = '2024-06-15';
            const result = timeWindowToDateParams('probehle', today);
            expect(result).toEqual({dateFrom: undefined, dateTo: '2024-06-14'});
        });

        it('maps Vše to neither dateFrom nor dateTo', () => {
            const today = '2024-06-15';
            const result = timeWindowToDateParams('vse', today);
            expect(result).toEqual({dateFrom: undefined, dateTo: undefined});
        });

        it('computes yesterday correctly for Jan 1st (year boundary)', () => {
            const today = '2024-01-01';
            const result = timeWindowToDateParams('probehle', today);
            expect(result).toEqual({dateFrom: undefined, dateTo: '2023-12-31'});
        });

        it('computes yesterday correctly for Mar 1st in leap year', () => {
            const today = '2024-03-01';
            const result = timeWindowToDateParams('probehle', today);
            expect(result).toEqual({dateFrom: undefined, dateTo: '2024-02-29'});
        });
    });

    describe('getDefaultSortForTimeWindow', () => {
        it('returns eventDate,asc for Budoucí', () => {
            expect(getDefaultSortForTimeWindow('budouci')).toEqual({by: 'eventDate', direction: 'asc'});
        });

        it('returns eventDate,desc for Proběhlé', () => {
            expect(getDefaultSortForTimeWindow('probehle')).toEqual({by: 'eventDate', direction: 'desc'});
        });

        it('returns eventDate,desc for Vše', () => {
            expect(getDefaultSortForTimeWindow('vse')).toEqual({by: 'eventDate', direction: 'desc'});
        });
    });

    describe('getTimeWindowFromParams', () => {
        it('detects Budoucí when dateFrom is set and dateTo is absent', () => {
            expect(getTimeWindowFromParams('2024-06-15', undefined)).toBe('budouci');
        });

        it('detects Proběhlé when dateTo is set and dateFrom is absent', () => {
            expect(getTimeWindowFromParams(undefined, '2024-06-14')).toBe('probehle');
        });

        it('detects Vše when neither dateFrom nor dateTo is set', () => {
            expect(getTimeWindowFromParams(undefined, undefined)).toBe('vse');
        });

        it('returns vse when neither param is set', () => {
            expect(getTimeWindowFromParams(null, null)).toBe('vse');
        });
    });

    describe('DEFAULT_TIME_WINDOW', () => {
        it('is budouci', () => {
            const expected: TimeWindow = 'budouci';
            expect(DEFAULT_TIME_WINDOW).toBe(expected);
        });
    });

    describe('yearToDateParams', () => {
        it('returns dateFrom=YYYY-01-01 and dateTo=YYYY-12-31 for a given year', () => {
            expect(yearToDateParams(2024)).toEqual({dateFrom: '2024-01-01', dateTo: '2024-12-31'});
        });

        it('handles the current century years correctly', () => {
            expect(yearToDateParams(2000)).toEqual({dateFrom: '2000-01-01', dateTo: '2000-12-31'});
            expect(yearToDateParams(2099)).toEqual({dateFrom: '2099-01-01', dateTo: '2099-12-31'});
        });
    });

    describe('getYearFromParams', () => {
        it('returns the year when dateFrom=YYYY-01-01 and dateTo=YYYY-12-31 match the same year', () => {
            expect(getYearFromParams('2024-01-01', '2024-12-31')).toBe(2024);
        });

        it('returns null when dateFrom and dateTo cover different years', () => {
            expect(getYearFromParams('2024-01-01', '2025-12-31')).toBeNull();
        });

        it('returns null when dateFrom is not YYYY-01-01', () => {
            expect(getYearFromParams('2024-03-01', '2024-12-31')).toBeNull();
        });

        it('returns null when dateTo is not YYYY-12-31', () => {
            expect(getYearFromParams('2024-01-01', '2024-11-30')).toBeNull();
        });

        it('returns null when either param is null/undefined', () => {
            expect(getYearFromParams(null, '2024-12-31')).toBeNull();
            expect(getYearFromParams('2024-01-01', null)).toBeNull();
            expect(getYearFromParams(null, null)).toBeNull();
            expect(getYearFromParams(undefined, undefined)).toBeNull();
        });
    });

    describe('getYearRange', () => {
        it('returns range from currentYear-10 to currentYear+2', () => {
            const currentYear = new Date().getFullYear();
            const range = getYearRange();
            expect(range[0]).toBe(currentYear - 10);
            expect(range[range.length - 1]).toBe(currentYear + 2);
            expect(range).toHaveLength(13);
        });

        it('returns years in ascending order', () => {
            const range = getYearRange();
            for (let i = 1; i < range.length; i++) {
                expect(range[i]).toBe(range[i - 1] + 1);
            }
        });
    });
});
