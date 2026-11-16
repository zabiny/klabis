import {describe, it, expect, vi, beforeEach} from 'vitest';
import {renderHook} from '@testing-library/react';
import {useEventTypes} from './useEventTypes.ts';
import {useHalCollection} from './useHalCollection.ts';

vi.mock('./useHalCollection.ts', () => ({
    useHalCollection: vi.fn(),
}));

const buildEventTypeList = (...names: string[]) =>
    names.map((name, i) => ({id: `type-${i + 1}`, name, color: `#FF${i}000`, sortOrder: i + 1}));

const mockCollection = (eventTypes: ReturnType<typeof buildEventTypeList>) => {
    vi.mocked(useHalCollection).mockReturnValue({
        items: eventTypes,
        pageMetadata: undefined,
        isLoading: false,
        error: null,
        refetch: vi.fn(),
    });
};

describe('useEventTypes', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('getById returns matching event type', () => {
        mockCollection(buildEventTypeList('Trénink', 'Závod'));
        const {result} = renderHook(() => useEventTypes());
        expect(result.current.getById('type-1')?.name).toBe('Trénink');
    });

    it('getById returns undefined for unknown id', () => {
        mockCollection(buildEventTypeList('Trénink'));
        const {result} = renderHook(() => useEventTypes());
        expect(result.current.getById('unknown-id')).toBeUndefined();
    });

    it('getById returns undefined for null/undefined id', () => {
        mockCollection(buildEventTypeList('Trénink'));
        const {result} = renderHook(() => useEventTypes());
        expect(result.current.getById(null)).toBeUndefined();
        expect(result.current.getById(undefined)).toBeUndefined();
    });

    it('queries /api/event-types with 5-minute staleTime', () => {
        mockCollection([]);
        renderHook(() => useEventTypes());
        expect(vi.mocked(useHalCollection)).toHaveBeenCalledWith(
            '/api/event-types',
            expect.objectContaining({staleTime: 5 * 60 * 1000}),
        );
    });
});
