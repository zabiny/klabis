import {renderHook} from '@testing-library/react';
import {vi} from 'vitest';
import {useLegalGuardians} from './useLegalGuardians';

vi.mock('./useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

import {useAuthorizedQuery} from './useAuthorizedFetch';

const mockUseAuthorizedQuery = vi.mocked(useAuthorizedQuery);

const respondWithGuardians = (names: string[]) => {
    mockUseAuthorizedQuery.mockImplementation((_url, options) => ({
        data: options?.select ? options.select({
            _links: {self: {href: '/api/legal-guardian-groups/g-1/guardians'}},
            _embedded: {legalGuardianGroupGuardianResponseList: names.map((n, i) => ({userId: `u-${i}`, firstName: n}))},
        }) : undefined,
        isLoading: false,
        error: null,
        refetch: vi.fn(),
    }) as unknown as ReturnType<typeof useAuthorizedQuery>);
};

describe('useLegalGuardians', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('queries the href behind the legalGuardians link', () => {
        respondWithGuardians([]);

        renderHook(() => useLegalGuardians({href: '/api/legal-guardian-groups/g-1/guardians'}));

        expect(mockUseAuthorizedQuery.mock.calls[0][0]).toBe('/api/legal-guardian-groups/g-1/guardians');
    });

    it('queries nothing and returns no guardians without the legalGuardians link', () => {
        respondWithGuardians([]);

        const {result} = renderHook(() => useLegalGuardians(undefined));

        expect(mockUseAuthorizedQuery.mock.calls[0][0]).toBeUndefined();
        expect(result.current.guardians).toEqual([]);
    });

    it('returns the guardians of the collection', () => {
        respondWithGuardians(['Jana', 'Eva']);

        const {result} = renderHook(() => useLegalGuardians({href: '/api/legal-guardian-groups/g-1/guardians'}));

        expect(result.current.guardians.map(g => g.firstName)).toEqual(['Jana', 'Eva']);
        expect(result.current.isLoading).toBe(false);
    });
});
