import {renderHook} from '@testing-library/react';
import {vi} from 'vitest';
import {useMembershipFeeTypeOptions} from './useMembershipFeeTypeOptions';

vi.mock('./useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

import {useAuthorizedQuery} from './useAuthorizedFetch';

const mockUseAuthorizedQuery = vi.mocked(useAuthorizedQuery);

const respondWithTypes = (types: {id?: string; name?: string}[]) => {
    mockUseAuthorizedQuery.mockReturnValue({
        data: {
            _links: {self: {href: '/api/membership-fee-types'}},
            _embedded: {membershipFeeTypeSummaryResponseList: types},
        },
        isLoading: false,
        error: null,
        refetch: vi.fn(),
    } as unknown as ReturnType<typeof useAuthorizedQuery>);
};

describe('useMembershipFeeTypeOptions', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('maps types to value/prompt options', () => {
        respondWithTypes([
            {id: 'uuid-1', name: 'Základní'},
            {id: 'uuid-2', name: 'Premium'},
        ]);

        const {result} = renderHook(() => useMembershipFeeTypeOptions());

        expect(result.current).toEqual([
            {value: 'uuid-1', prompt: 'Základní'},
            {value: 'uuid-2', prompt: 'Premium'},
        ]);
    });

    it('filters out types with a missing id', () => {
        respondWithTypes([
            {id: undefined, name: 'No ID type'},
            {id: 'uuid-valid', name: 'Valid type'},
        ]);

        const {result} = renderHook(() => useMembershipFeeTypeOptions());

        expect(result.current).toEqual([{value: 'uuid-valid', prompt: 'Valid type'}]);
    });

    it('filters out types with a missing name', () => {
        respondWithTypes([
            {id: 'uuid-no-name', name: undefined},
            {id: 'uuid-ok', name: 'OK type'},
        ]);

        const {result} = renderHook(() => useMembershipFeeTypeOptions());

        expect(result.current).toEqual([{value: 'uuid-ok', prompt: 'OK type'}]);
    });

    it('queries /api/membership-fee-types with a 5-minute staleTime', () => {
        respondWithTypes([]);

        renderHook(() => useMembershipFeeTypeOptions());

        expect(mockUseAuthorizedQuery).toHaveBeenCalledWith(
            '/api/membership-fee-types',
            expect.objectContaining({staleTime: 5 * 60 * 1000}),
        );
    });
});
