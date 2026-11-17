import {describe, it, expect, vi, beforeEach} from 'vitest';
import {renderHook} from '@testing-library/react';
import {useEventTypes} from './useEventTypes.ts';

vi.mock('./useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

import {useAuthorizedQuery} from './useAuthorizedFetch';

const mockUseAuthorizedQuery = vi.mocked(useAuthorizedQuery);

const buildEventTypeList = (...names: string[]) =>
    names.map((name, i) => ({id: `type-${i + 1}`, name, color: `#FF${i}000`, sortOrder: i + 1}));

const respondWithEventTypes = (eventTypes: ReturnType<typeof buildEventTypeList>) => {
    mockUseAuthorizedQuery.mockReturnValue({
        data: {
            _links: {self: {href: '/api/event-types'}},
            _embedded: {eventTypeDtoList: eventTypes},
        },
        isLoading: false,
        error: null,
        refetch: vi.fn(),
    } as unknown as ReturnType<typeof useAuthorizedQuery>);
};

describe('useEventTypes', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('getById returns matching event type', () => {
        respondWithEventTypes(buildEventTypeList('Trénink', 'Závod'));
        const {result} = renderHook(() => useEventTypes());
        expect(result.current.getById('type-1')?.name).toBe('Trénink');
    });

    it('getById returns undefined for unknown id', () => {
        respondWithEventTypes(buildEventTypeList('Trénink'));
        const {result} = renderHook(() => useEventTypes());
        expect(result.current.getById('unknown-id')).toBeUndefined();
    });

    it('getById returns undefined for null/undefined id', () => {
        respondWithEventTypes(buildEventTypeList('Trénink'));
        const {result} = renderHook(() => useEventTypes());
        expect(result.current.getById(null)).toBeUndefined();
        expect(result.current.getById(undefined)).toBeUndefined();
    });

    it('queries /api/event-types with 5-minute staleTime', () => {
        respondWithEventTypes([]);
        renderHook(() => useEventTypes());
        expect(mockUseAuthorizedQuery).toHaveBeenCalledWith('/api/event-types', {staleTime: 5 * 60 * 1000});
    });
});
