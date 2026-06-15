import {renderHook} from '@testing-library/react';
import {vi} from 'vitest';
import {useMembershipFeeTierOptions} from './useMembershipFeeTierOptions';

vi.mock('./useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

import {useAuthorizedQuery} from './useAuthorizedFetch';

const mockUseAuthorizedQuery = vi.mocked(useAuthorizedQuery);

describe('useMembershipFeeTierOptions', () => {

    it('should return empty array when data is loading', () => {
        mockUseAuthorizedQuery.mockReturnValue({data: undefined, isLoading: true} as ReturnType<typeof useAuthorizedQuery>);

        const {result} = renderHook(() => useMembershipFeeTierOptions());

        expect(result.current).toEqual([]);
    });

    it('should return empty array when embedded list is absent', () => {
        mockUseAuthorizedQuery.mockReturnValue({data: {}, isLoading: false} as ReturnType<typeof useAuthorizedQuery>);

        const {result} = renderHook(() => useMembershipFeeTierOptions());

        expect(result.current).toEqual([]);
    });

    it('should map tiers to value/prompt options', () => {
        mockUseAuthorizedQuery.mockReturnValue({
            data: {
                _embedded: {
                    membershipFeeTierSummaryResponseList: [
                        {id: 'uuid-1', name: 'Základní'},
                        {id: 'uuid-2', name: 'Premium'},
                    ],
                },
            },
            isLoading: false,
        } as ReturnType<typeof useAuthorizedQuery>);

        const {result} = renderHook(() => useMembershipFeeTierOptions());

        expect(result.current).toEqual([
            {value: 'uuid-1', prompt: 'Základní'},
            {value: 'uuid-2', prompt: 'Premium'},
        ]);
    });

    it('should filter out entries with missing id', () => {
        mockUseAuthorizedQuery.mockReturnValue({
            data: {
                _embedded: {
                    membershipFeeTierSummaryResponseList: [
                        {id: undefined, name: 'No ID tier'},
                        {id: 'uuid-valid', name: 'Valid tier'},
                    ],
                },
            },
            isLoading: false,
        } as ReturnType<typeof useAuthorizedQuery>);

        const {result} = renderHook(() => useMembershipFeeTierOptions());

        expect(result.current).toEqual([{value: 'uuid-valid', prompt: 'Valid tier'}]);
    });

    it('should filter out entries with missing name', () => {
        mockUseAuthorizedQuery.mockReturnValue({
            data: {
                _embedded: {
                    membershipFeeTierSummaryResponseList: [
                        {id: 'uuid-no-name', name: undefined},
                        {id: 'uuid-ok', name: 'OK tier'},
                    ],
                },
            },
            isLoading: false,
        } as ReturnType<typeof useAuthorizedQuery>);

        const {result} = renderHook(() => useMembershipFeeTierOptions());

        expect(result.current).toEqual([{value: 'uuid-ok', prompt: 'OK tier'}]);
    });

    it('should call useAuthorizedQuery with /api/membership-fee-tiers endpoint', () => {
        mockUseAuthorizedQuery.mockReturnValue({data: undefined, isLoading: false} as ReturnType<typeof useAuthorizedQuery>);

        renderHook(() => useMembershipFeeTierOptions());

        expect(mockUseAuthorizedQuery).toHaveBeenCalledWith('/api/membership-fee-tiers', expect.any(Object));
    });
});
