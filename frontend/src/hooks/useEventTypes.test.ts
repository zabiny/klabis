import {describe, it, expect, vi, beforeEach} from 'vitest';
import {renderHook} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {UseQueryResult} from '@tanstack/react-query';
import {createElement} from 'react';
import {useEventTypes} from './useEventTypes.ts';
import {useAuthorizedQuery} from './useAuthorizedFetch.ts';

vi.mock('./useAuthorizedFetch.ts', () => ({
    useAuthorizedQuery: vi.fn(),
}));

const buildEventTypeList = (...names: string[]) =>
    names.map((name, i) => ({id: `type-${i + 1}`, name, color: `#FF${i}000`, sortOrder: i + 1}));

const createWrapper = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});
    return ({children}: {children: React.ReactNode}) =>
        createElement(QueryClientProvider, {client: queryClient}, children);
};

describe('useEventTypes', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('returns empty list when no data', () => {
        vi.mocked(useAuthorizedQuery).mockReturnValue({data: undefined, isLoading: false, error: null} as unknown as UseQueryResult<unknown>);
        const {result} = renderHook(() => useEventTypes(), {wrapper: createWrapper()});
        expect(result.current.eventTypes).toEqual([]);
    });

    it('returns event types from embedded list', () => {
        const types = buildEventTypeList('Trénink', 'Závod');
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: {_embedded: {eventTypeDtoList: types}},
            isLoading: false,
            error: null,
        } as unknown as UseQueryResult<unknown>);
        const {result} = renderHook(() => useEventTypes(), {wrapper: createWrapper()});
        expect(result.current.eventTypes).toHaveLength(2);
        expect(result.current.eventTypes[0].name).toBe('Trénink');
    });

    it('getById returns matching event type', () => {
        const types = buildEventTypeList('Trénink', 'Závod');
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: {_embedded: {eventTypeDtoList: types}},
            isLoading: false,
            error: null,
        } as unknown as UseQueryResult<unknown>);
        const {result} = renderHook(() => useEventTypes(), {wrapper: createWrapper()});
        expect(result.current.getById('type-1')?.name).toBe('Trénink');
    });

    it('getById returns undefined for unknown id', () => {
        const types = buildEventTypeList('Trénink');
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: {_embedded: {eventTypeDtoList: types}},
            isLoading: false,
            error: null,
        } as unknown as UseQueryResult<unknown>);
        const {result} = renderHook(() => useEventTypes(), {wrapper: createWrapper()});
        expect(result.current.getById('unknown-id')).toBeUndefined();
    });

    it('getById returns undefined for null/undefined id', () => {
        const types = buildEventTypeList('Trénink');
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: {_embedded: {eventTypeDtoList: types}},
            isLoading: false,
            error: null,
        } as unknown as UseQueryResult<unknown>);
        const {result} = renderHook(() => useEventTypes(), {wrapper: createWrapper()});
        expect(result.current.getById(null)).toBeUndefined();
        expect(result.current.getById(undefined)).toBeUndefined();
    });

    it('queries /api/event-types with 5-minute staleTime', () => {
        vi.mocked(useAuthorizedQuery).mockReturnValue({data: undefined, isLoading: false, error: null} as unknown as UseQueryResult<unknown>);
        renderHook(() => useEventTypes(), {wrapper: createWrapper()});
        expect(vi.mocked(useAuthorizedQuery)).toHaveBeenCalledWith(
            '/api/event-types',
            expect.objectContaining({staleTime: 5 * 60 * 1000}),
        );
    });
});
