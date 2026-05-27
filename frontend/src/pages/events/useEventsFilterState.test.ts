import {renderHook, act} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {createElement} from 'react';
import {describe, expect, it, vi, beforeEach, afterEach} from 'vitest';
import {useEventsFilterState} from './useEventsFilterState';

function wrapper(initialUrl: string) {
    return ({children}: {children: React.ReactNode}) =>
        createElement(MemoryRouter, {initialEntries: [initialUrl]}, children);
}

describe('useEventsFilterState — basic URL load (legacy)', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2024-06-15'));
    });
    afterEach(() => {
        vi.useRealTimers();
    });

    describe('URL load: no year or when params means null/default', () => {
        it('shows null selectedYear when no ?year= in URL', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.selectedYear).toBeNull();
        });
    });

    describe('year selection: extraParams contain AND-combined dates', () => {
        it('year=2023 + vse: extraParams dateFrom=2023-01-01, dateTo=2023-12-31', () => {
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

    describe('clear year: removes year and time-window reverts to no-year behavior', () => {
        it('extraParams has no dateTo when year is cleared and budouci is active', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=budouci'),
            });
            act(() => {
                result.current.handleFilterChange({
                    ...result.current.filterValue,
                    selectedYear: null,
                    timeWindow: 'budouci',
                });
            });
            expect(result.current.extraParams.dateTo).toBeUndefined();
            expect(result.current.filterValue.selectedYear).toBeNull();
            expect(result.current.filterValue.timeWindow).toBe('budouci');
        });

        it('removes both dateFrom and dateTo when year is cleared and vse is active', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=vse'),
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

    describe('time window change does NOT clear year', () => {
        it('year remains set when timeWindow changes', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=vse'),
            });
            act(() => {
                result.current.handleFilterChange({
                    ...result.current.filterValue,
                    selectedYear: 2024,
                    timeWindow: 'budouci',
                });
            });
            expect(result.current.filterValue.selectedYear).toBe(2024);
            expect(result.current.filterValue.timeWindow).toBe('budouci');
        });
    });
});

describe('useEventsFilterState — AND semantics (year + time window independent)', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2024-06-15'));
    });
    afterEach(() => {
        vi.useRealTimers();
    });

    describe('URL load: independent year and when params', () => {
        it('reads year from ?year= param', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=budouci'),
            });
            expect(result.current.filterValue.selectedYear).toBe(2024);
        });

        it('reads timeWindow from ?when= param', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=budouci'),
            });
            expect(result.current.filterValue.timeWindow).toBe('budouci');
        });

        it('reads probehle from ?when=probehle', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=probehle'),
            });
            expect(result.current.filterValue.timeWindow).toBe('probehle');
        });

        it('reads vse from ?when=vse', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?when=vse'),
            });
            expect(result.current.filterValue.timeWindow).toBe('vse');
        });

        it('year=null when no ?year= param', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?when=budouci'),
            });
            expect(result.current.filterValue.selectedYear).toBeNull();
        });
    });

    describe('AND combination: extraParams contain AND-combined dateFrom/dateTo', () => {
        it('year=2024 + budouci: dateFrom=today, dateTo=2024-12-31', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=budouci'),
            });
            expect(result.current.extraParams.dateFrom).toBe('2024-06-15');
            expect(result.current.extraParams.dateTo).toBe('2024-12-31');
        });

        it('year=2024 + probehle: dateFrom=2024-01-01, dateTo=yesterday', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=probehle'),
            });
            expect(result.current.extraParams.dateFrom).toBe('2024-01-01');
            expect(result.current.extraParams.dateTo).toBe('2024-06-14');
        });

        it('year=2024 + vse: dateFrom=2024-01-01, dateTo=2024-12-31', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=vse'),
            });
            expect(result.current.extraParams.dateFrom).toBe('2024-01-01');
            expect(result.current.extraParams.dateTo).toBe('2024-12-31');
        });

        it('no year + budouci: only dateFrom=today', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?when=budouci'),
            });
            expect(result.current.extraParams.dateFrom).toBe('2024-06-15');
            expect(result.current.extraParams.dateTo).toBeUndefined();
        });

        it('no year + probehle: only dateTo=yesterday', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?when=probehle'),
            });
            expect(result.current.extraParams.dateFrom).toBeUndefined();
            expect(result.current.extraParams.dateTo).toBe('2024-06-14');
        });

        it('no year + vse: no date constraints', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?when=vse'),
            });
            expect(result.current.extraParams.dateFrom).toBeUndefined();
            expect(result.current.extraParams.dateTo).toBeUndefined();
        });
    });

    describe('handleFilterChange: writes ?year= and ?when= params independently', () => {
        it('selecting a year does NOT reset the timeWindow', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?when=budouci'),
            });
            act(() => {
                result.current.handleFilterChange({
                    ...result.current.filterValue,
                    selectedYear: 2024,
                    timeWindow: 'budouci',
                });
            });
            expect(result.current.filterValue.timeWindow).toBe('budouci');
            expect(result.current.filterValue.selectedYear).toBe(2024);
        });

        it('changing timeWindow does NOT reset the selectedYear', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=vse'),
            });
            act(() => {
                result.current.handleFilterChange({
                    ...result.current.filterValue,
                    selectedYear: 2024,
                    timeWindow: 'probehle',
                });
            });
            expect(result.current.filterValue.selectedYear).toBe(2024);
            expect(result.current.filterValue.timeWindow).toBe('probehle');
        });

        it('writes ?year=2024&when=budouci to URL when both are set', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/'),
            });
            act(() => {
                result.current.handleFilterChange({
                    ...result.current.filterValue,
                    selectedYear: 2024,
                    timeWindow: 'budouci',
                });
            });
            expect(result.current.filterValue.selectedYear).toBe(2024);
            expect(result.current.filterValue.timeWindow).toBe('budouci');
            // AND combination: extraParams must reflect AND of year + budouci
            expect(result.current.extraParams.dateFrom).toBe('2024-06-15');
            expect(result.current.extraParams.dateTo).toBe('2024-12-31');
        });

        it('clears ?year= from URL when selectedYear is set to null', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=budouci'),
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
