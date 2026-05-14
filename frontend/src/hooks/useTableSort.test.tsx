import '@testing-library/jest-dom';
import { renderHook, act } from '@testing-library/react';
import { MemoryRouter, useSearchParams } from 'react-router-dom';
import { describe, it, expect, beforeEach } from 'vitest';
import type { ReactNode } from 'react';
import { useTableSort } from './useTableSort';

const DEFAULT_SORT = { by: 'name', direction: 'asc' as const };

const makeWrapper = (initialUrl: string) =>
    ({ children }: { children: ReactNode }) => (
        <MemoryRouter initialEntries={[initialUrl]}>{children}</MemoryRouter>
    );

/** Helper to read the current URL search params from within the hook's router context */
function renderTableSortHook(initialUrl: string, tableId = 'test-table', defaultSort = DEFAULT_SORT) {
    return renderHook(() => useTableSort(tableId, defaultSort), {
        wrapper: makeWrapper(initialUrl),
    });
}

/** Helper to read URL sort param after hook actions */
function renderWithUrlReader(initialUrl: string, tableId = 'test-table', defaultSort = DEFAULT_SORT) {
    let capturedSearchParams: URLSearchParams | null = null;

    const UrlCapture = ({ children }: { children: ReactNode }) => {
        const [searchParams] = useSearchParams();
        capturedSearchParams = searchParams;
        return <>{children}</>;
    };

    const wrapper = ({ children }: { children: ReactNode }) => (
        <MemoryRouter initialEntries={[initialUrl]}>
            <UrlCapture>{children}</UrlCapture>
        </MemoryRouter>
    );

    const result = renderHook(() => useTableSort(tableId, defaultSort), { wrapper });
    return { ...result, getUrlSort: () => capturedSearchParams?.get('sort') ?? null };
}

describe('useTableSort', () => {
    beforeEach(() => {
        localStorage.clear();
    });

    describe('initial state resolution', () => {
        it('uses URL sort when URL has ?sort param', () => {
            const { result } = renderTableSortHook('/?sort=date,desc');
            expect(result.current.sort).toEqual({ by: 'date', direction: 'desc' });
        });

        it('uses localStorage sort when no URL param is present', () => {
            localStorage.setItem('klabis.table.test-table.sort', 'date,desc');
            const { result } = renderTableSortHook('/');
            expect(result.current.sort).toEqual({ by: 'date', direction: 'desc' });
        });

        it('URL sort wins over localStorage when both are present', () => {
            localStorage.setItem('klabis.table.test-table.sort', 'email,asc');
            const { result } = renderTableSortHook('/?sort=date,desc');
            expect(result.current.sort).toEqual({ by: 'date', direction: 'desc' });
        });

        it('falls back to defaultSort when neither URL nor localStorage is present', () => {
            const { result } = renderTableSortHook('/');
            expect(result.current.sort).toEqual(DEFAULT_SORT);
        });

        it('falls back to defaultSort on invalid localStorage value (bad format)', () => {
            localStorage.setItem('klabis.table.test-table.sort', 'invalid-no-direction');
            const { result } = renderTableSortHook('/');
            expect(result.current.sort).toEqual(DEFAULT_SORT);
        });

        it('falls back to defaultSort when localStorage direction is not asc or desc', () => {
            localStorage.setItem('klabis.table.test-table.sort', 'name,random');
            const { result } = renderTableSortHook('/');
            expect(result.current.sort).toEqual(DEFAULT_SORT);
        });

        it('falls back to defaultSort when localStorage has empty column name', () => {
            localStorage.setItem('klabis.table.test-table.sort', ',asc');
            const { result } = renderTableSortHook('/');
            expect(result.current.sort).toEqual(DEFAULT_SORT);
        });

        it('falls back to defaultSort when localStorage value is empty string', () => {
            localStorage.setItem('klabis.table.test-table.sort', '');
            const { result } = renderTableSortHook('/');
            expect(result.current.sort).toEqual(DEFAULT_SORT);
        });
    });

    describe('setSort', () => {
        it('updates the sort state', () => {
            const { result } = renderTableSortHook('/');
            act(() => {
                result.current.setSort({ by: 'email', direction: 'desc' });
            });
            expect(result.current.sort).toEqual({ by: 'email', direction: 'desc' });
        });

        it('writes to localStorage with key klabis.table.<tableId>.sort', () => {
            const { result } = renderTableSortHook('/', 'members');
            act(() => {
                result.current.setSort({ by: 'lastName', direction: 'asc' });
            });
            expect(localStorage.getItem('klabis.table.members.sort')).toBe('lastName,asc');
        });

        it('writes plain string (not JSON) to localStorage', () => {
            const { result } = renderTableSortHook('/');
            act(() => {
                result.current.setSort({ by: 'date', direction: 'desc' });
            });
            const stored = localStorage.getItem('klabis.table.test-table.sort');
            expect(stored).toBe('date,desc');
            // Ensure it is NOT JSON-encoded
            expect(() => JSON.parse(stored!)).toThrow();
        });

        it('updates URL ?sort param', () => {
            const { result, getUrlSort } = renderWithUrlReader('/');
            act(() => {
                result.current.setSort({ by: 'date', direction: 'desc' });
            });
            expect(getUrlSort()).toBe('date,desc');
        });
    });

    describe('reset', () => {
        it('resets sort state to defaultSort', () => {
            localStorage.setItem('klabis.table.test-table.sort', 'email,desc');
            const { result } = renderTableSortHook('/');
            act(() => {
                result.current.reset();
            });
            expect(result.current.sort).toEqual(DEFAULT_SORT);
        });

        it('removes localStorage entry', () => {
            localStorage.setItem('klabis.table.test-table.sort', 'email,desc');
            const { result } = renderTableSortHook('/');
            act(() => {
                result.current.reset();
            });
            expect(localStorage.getItem('klabis.table.test-table.sort')).toBeNull();
        });

        it('removes ?sort param from URL', () => {
            const { result, getUrlSort } = renderWithUrlReader('/?sort=email,desc');
            act(() => {
                result.current.reset();
            });
            expect(getUrlSort()).toBeNull();
        });
    });

    describe('tableId isolation', () => {
        it('different tableIds use independent localStorage keys', () => {
            const { result: result1 } = renderTableSortHook('/', 'events');
            const { result: result2 } = renderTableSortHook('/', 'members');

            act(() => {
                result1.current.setSort({ by: 'date', direction: 'desc' });
            });
            act(() => {
                result2.current.setSort({ by: 'lastName', direction: 'asc' });
            });

            expect(localStorage.getItem('klabis.table.events.sort')).toBe('date,desc');
            expect(localStorage.getItem('klabis.table.members.sort')).toBe('lastName,asc');
        });
    });
});
