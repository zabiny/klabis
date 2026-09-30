import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import {act} from 'react';
import {vi} from 'vitest';
import {authorizedFetch} from '../api/authorizedFetch';
import {useLegalGuardians} from './useLegalGuardians';
import {useFormCacheInvalidation} from './useFormCacheInvalidation';

vi.mock('../api/authorizedFetch', () => ({authorizedFetch: vi.fn()}));

const respond = (names: string[]) => vi.mocked(authorizedFetch).mockResolvedValue({
    json: async () => ({
        _embedded: {legalGuardianGroupGuardianResponseList: names.map((n, i) => ({userId: `u-${i}`, firstName: n}))},
    }),
} as Response);

describe('useLegalGuardians', () => {
    it('does not fetch without the legalGuardians link', () => {
        const client = new QueryClient();
        const wrapper = ({children}: {children: ReactNode}) => <QueryClientProvider client={client}>{children}</QueryClientProvider>;
        const {result} = renderHook(() => useLegalGuardians(undefined), {wrapper});
        expect(result.current.guardians).toEqual([]);
        expect(authorizedFetch).not.toHaveBeenCalled();
    });

    it('refetches guardians after form cache invalidation (as done after a successful form submit)', async () => {
        const client = new QueryClient({defaultOptions: {queries: {retry: false}}});
        const wrapper = ({children}: {children: ReactNode}) => <QueryClientProvider client={client}>{children}</QueryClientProvider>;
        respond(['Jana']);

        const {result} = renderHook(() => ({
            guardians: useLegalGuardians({href: '/api/legal-guardian-groups/g-1/guardians'}),
            cache: useFormCacheInvalidation(),
        }), {wrapper});
        await waitFor(() => expect(result.current.guardians.guardians).toHaveLength(1));

        respond(['Jana', 'Eva']);
        await act(async () => {
            await result.current.cache.invalidateAllCaches();
        });

        await waitFor(() => expect(result.current.guardians.guardians).toHaveLength(2));
    });
});
