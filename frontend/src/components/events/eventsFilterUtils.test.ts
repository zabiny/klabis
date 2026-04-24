import {describe, expect, it} from 'vitest';
import {
    DEFAULT_TIME_WINDOW,
    getDefaultSortForTimeWindow,
    getTimeWindowFromParams,
    timeWindowToDateParams,
    type TimeWindow,
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

        it('returns Budoucí as default when neither param is set (default state)', () => {
            expect(getTimeWindowFromParams(null, null)).toBe('vse');
        });
    });

    describe('DEFAULT_TIME_WINDOW', () => {
        it('is budouci', () => {
            const expected: TimeWindow = 'budouci';
            expect(DEFAULT_TIME_WINDOW).toBe(expected);
        });
    });
});
