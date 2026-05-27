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
        it('defaults selectedYear to current year when no ?year= in URL', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.selectedYear).toBe(2024);
        });

        it('defaults timeWindow to DEFAULT_TIME_WINDOW (budouci) when no ?when= in URL', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.timeWindow).toBe('budouci');
        });
    });

    describe('default year — URL param semantics', () => {
        it('keeps selectedYear=2024 when ?year=2024 is present (explicit param overrides default)', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=budouci'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.selectedYear).toBe(2024);
        });

        it('keeps selectedYear=null when ?year= is present but empty (sentinel for no-year)', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=&when=vse'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.selectedYear).toBeNull();
        });

        it('does NOT replace ?year=2024 with current year on first load', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.selectedYear).toBe(2024);
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

        it('year defaults to current year when no ?year= param', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?when=budouci'),
            });
            expect(result.current.filterValue.selectedYear).toBe(2024);
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

        it('explicit no-year (?year=) + budouci: only dateFrom=today (no upper bound)', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=&when=budouci'),
            });
            expect(result.current.extraParams.dateFrom).toBe('2024-06-15');
            expect(result.current.extraParams.dateTo).toBeUndefined();
        });

        it('explicit no-year (?year=) + probehle: only dateTo=yesterday (no lower bound)', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=&when=probehle'),
            });
            expect(result.current.extraParams.dateFrom).toBeUndefined();
            expect(result.current.extraParams.dateTo).toBe('2024-06-14');
        });

        it('explicit no-year (?year=) + vse: no date constraints', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=&when=vse'),
            });
            expect(result.current.extraParams.dateFrom).toBeUndefined();
            expect(result.current.extraParams.dateTo).toBeUndefined();
        });
    });

    describe('URL normalisation: non-current year + budouci/probehle coerced to vse', () => {
        beforeEach(() => {
            vi.useFakeTimers();
            vi.setSystemTime(new Date('2026-06-15'));
        });

        it('?year=2024&when=budouci: coerces timeWindow to vse, extraParams has only year-bounded dates', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=budouci'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.timeWindow).toBe('vse');
            expect(result.current.extraParams.dateFrom).toBe('2024-01-01');
            expect(result.current.extraParams.dateTo).toBe('2024-12-31');
        });

        it('?year=2027&when=probehle (future year): coerces timeWindow to vse, URL normalised', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2027&when=probehle'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.timeWindow).toBe('vse');
            expect(result.current.extraParams.dateFrom).toBe('2027-01-01');
            expect(result.current.extraParams.dateTo).toBe('2027-12-31');
        });

        it('?year=2026&when=budouci (current year): keeps both filters active, dateFrom=today', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2026&when=budouci'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.timeWindow).toBe('budouci');
            expect(result.current.filterValue.selectedYear).toBe(2026);
            expect(result.current.extraParams.dateFrom).toBe('2026-06-15');
            expect(result.current.extraParams.dateTo).toBe('2026-12-31');
        });

        it('?year=&when=budouci (no-year sentinel): keeps budouci, no coercion', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=&when=budouci'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.timeWindow).toBe('budouci');
            expect(result.current.filterValue.selectedYear).toBeNull();
        });

        it('?year=2026&when=vse (current year + vse): reloads cleanly, both filters active', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2026&when=vse'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.timeWindow).toBe('vse');
            expect(result.current.filterValue.selectedYear).toBe(2026);
            expect(result.current.extraParams.dateFrom).toBe('2026-01-01');
            expect(result.current.extraParams.dateTo).toBe('2026-12-31');
        });
    });

    describe('Fix 1: bootstrap coercion when ?year=<non-current> has no ?when= param', () => {
        beforeEach(() => {
            vi.useFakeTimers();
            vi.setSystemTime(new Date('2026-06-15'));
        });

        it('?year=2024 (no when): coerces to when=vse on first render', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024'),
            });
            act(() => { vi.advanceTimersByTime(0); });
            expect(result.current.filterValue.timeWindow).toBe('vse');
            expect(result.current.filterValue.selectedYear).toBe(2024);
            expect(result.current.extraParams.dateFrom).toBe('2024-01-01');
            expect(result.current.extraParams.dateTo).toBe('2024-12-31');
        });
    });

    describe('Fix 2: ?year=<non-numeric> results in selectedYear===null', () => {
        it('?year=abc: selectedYear is null (not NaN)', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=abc&when=vse'),
            });
            expect(result.current.filterValue.selectedYear).toBeNull();
        });

        it('?year=abc: extraParams has no dateFrom/dateTo (no NaN in API params)', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=abc&when=vse'),
            });
            expect(result.current.extraParams.dateFrom).toBeUndefined();
            expect(result.current.extraParams.dateTo).toBeUndefined();
        });
    });

    describe('Fix 3: first render with non-current year + budouci/probehle uses effectiveTimeWindow', () => {
        beforeEach(() => {
            vi.useFakeTimers();
            vi.setSystemTime(new Date('2026-06-15'));
        });

        it('?year=2024&when=budouci: FIRST render extraParams.dateFrom is 2024-01-01, filterValue.timeWindow is vse', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=budouci'),
            });
            // No act() — checking FIRST render before effect fires
            expect(result.current.filterValue.timeWindow).toBe('vse');
            expect(result.current.extraParams.dateFrom).toBe('2024-01-01');
            expect(result.current.extraParams.dateTo).toBe('2024-12-31');
        });

        it('?year=2024&when=probehle: FIRST render extraParams.dateTo is 2024-12-31, filterValue.timeWindow is vse', () => {
            const {result} = renderHook(() => useEventsFilterState(), {
                wrapper: wrapper('/?year=2024&when=probehle'),
            });
            // No act() — checking FIRST render before effect fires
            expect(result.current.filterValue.timeWindow).toBe('vse');
            expect(result.current.extraParams.dateFrom).toBe('2024-01-01');
            expect(result.current.extraParams.dateTo).toBe('2024-12-31');
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
