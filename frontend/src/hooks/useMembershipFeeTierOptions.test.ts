import {renderHook} from '@testing-library/react';
import {vi} from 'vitest';
import {useMembershipFeeTierOptions} from './useMembershipFeeTierOptions';

vi.mock('./useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

import {useAuthorizedQuery} from './useAuthorizedFetch';

const mockUseAuthorizedQuery = vi.mocked(useAuthorizedQuery);

const respondWithTiers = (tiers: {id?: string; name?: string}[]) => {
    mockUseAuthorizedQuery.mockReturnValue({
        data: {
            _links: {self: {href: '/api/membership-fee-tiers'}},
            _embedded: {membershipFeeTierSummaryResponseList: tiers},
        },
        isLoading: false,
        error: null,
        refetch: vi.fn(),
    } as unknown as ReturnType<typeof useAuthorizedQuery>);
};

describe('useMembershipFeeTierOptions', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('maps tiers to value/prompt options', () => {
        respondWithTiers([
            {id: 'uuid-1', name: 'Základní'},
            {id: 'uuid-2', name: 'Premium'},
        ]);

        const {result} = renderHook(() => useMembershipFeeTierOptions());

        expect(result.current).toEqual([
            {value: 'uuid-1', prompt: 'Základní'},
            {value: 'uuid-2', prompt: 'Premium'},
        ]);
    });

    it('filters out tiers with a missing id', () => {
        respondWithTiers([
            {id: undefined, name: 'No ID tier'},
            {id: 'uuid-valid', name: 'Valid tier'},
        ]);

        const {result} = renderHook(() => useMembershipFeeTierOptions());

        expect(result.current).toEqual([{value: 'uuid-valid', prompt: 'Valid tier'}]);
    });

    it('filters out tiers with a missing name', () => {
        respondWithTiers([
            {id: 'uuid-no-name', name: undefined},
            {id: 'uuid-ok', name: 'OK tier'},
        ]);

        const {result} = renderHook(() => useMembershipFeeTierOptions());

        expect(result.current).toEqual([{value: 'uuid-ok', prompt: 'OK tier'}]);
    });

    it('queries /api/membership-fee-tiers with a 5-minute staleTime', () => {
        respondWithTiers([]);

        renderHook(() => useMembershipFeeTierOptions());

        expect(mockUseAuthorizedQuery).toHaveBeenCalledWith(
            '/api/membership-fee-tiers',
            expect.objectContaining({staleTime: 5 * 60 * 1000}),
        );
    });
});
