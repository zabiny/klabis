import {renderHook, act} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {createElement} from 'react';
import {describe, expect, it, vi, beforeEach, afterEach} from 'vitest';
import {useEventsFilterState} from './useEventsFilterState';

function wrapper(initialUrl: string) {
    return ({children}: {children: React.ReactNode}) =>
        createElement(MemoryRouter, {initialEntries: [initialUrl]}, children);
}

describe('useEventsFilterState — year filter', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2024-06-15'));
    });
    afterEach(() => {
        vi.useRealTimers();
    });

    describe('URL load: derive selected year from dateFrom/dateTo', () => {
        it('shows selected year when URL has a full calendar year range', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?dateFrom=2024-01-01&dateTo=2024-12-31'),
            });
            expect(result.current.filterValue.selectedYear).toBe(2024);
        });

        it('shows null when URL date range does not cover exactly one calendar year', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?dateFrom=2024-03-01&dateTo=2024-12-31'),
            });
            expect(result.current.filterValue.selectedYear).toBeNull();
        });

        it('shows null when no dateFrom/dateTo in URL', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.selectedYear).toBeNull();
        });

        it('shows null when only dateFrom is set (budouci time window)', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?dateFrom=2024-06-15'),
            });
            expect(result.current.filterValue.selectedYear).toBeNull();
        });
    });

    describe('year change: updates URL params and switches time window to vse', () => {
        it('sets dateFrom and dateTo in URL when a year is selected', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/'),
            });
            act(() => {
                result.current.handleFilterChange({
                    ...result.current.filterValue,
                    selectedYear: 2023,
                    timeWindow: 'vse',
                });
            });
            expect(result.current.extraParams.dateFrom).toBe('2023-01-01');
            expect(result.current.extraParams.dateTo).toBe('2023-12-31');
        });

        it('reflects selectedYear=2023 in filterValue after year selection', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/'),
            });
            act(() => {
                result.current.handleFilterChange({
                    ...result.current.filterValue,
                    selectedYear: 2023,
                    timeWindow: 'vse',
                });
            });
            expect(result.current.filterValue.selectedYear).toBe(2023);
        });
    });

    describe('clear year: removes year date range from URL', () => {
        it('removes year-based dateTo from URL when year is cleared and budouci is restored', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?dateFrom=2024-01-01&dateTo=2024-12-31'),
            });
            act(() => {
                result.current.handleFilterChange({
                    ...result.current.filterValue,
                    selectedYear: null,
                    timeWindow: 'budouci',
                });
            });
            // budouci sets dateFrom=today, removes dateTo
            expect(result.current.extraParams.dateTo).toBeUndefined();
            expect(result.current.filterValue.selectedYear).toBeNull();
            expect(result.current.filterValue.timeWindow).toBe('budouci');
        });

        it('removes both dateFrom and dateTo when year is cleared and vse is restored', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?dateFrom=2024-01-01&dateTo=2024-12-31'),
            });
            act(() => {
                result.current.handleFilterChange({
                    ...result.current.filterValue,
                    selectedYear: null,
                    timeWindow: 'vse',
                });
            });
            expect(result.current.extraParams.dateFrom).toBeUndefined();
            expect(result.current.extraParams.dateTo).toBeUndefined();
        });
    });

    describe('time window change clears year params', () => {
        it('removes year-based dateFrom/dateTo when timeWindow changes to budouci', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?dateFrom=2024-01-01&dateTo=2024-12-31'),
            });
            act(() => {
                result.current.handleFilterChange({
                    ...result.current.filterValue,
                    selectedYear: null,
                    timeWindow: 'budouci',
                });
            });
            expect(result.current.filterValue.selectedYear).toBeNull();
            expect(result.current.filterValue.timeWindow).toBe('budouci');
        });
    });
});
