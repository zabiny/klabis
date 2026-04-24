import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import React from 'react';
import {useDashboard} from './useDashboard';
import {vi} from 'vitest';

vi.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

const createWrapper = () => {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false, gcTime: 0}},
    });
    return ({children}: {children: ReactNode}) =>
        React.createElement(QueryClientProvider, {client: queryClient}, children);
};

const mockFetchWithResponse = (data: unknown) => {
    (globalThis as any).fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => data,
        headers: new Headers(),
        clone: function () { return this; },
    } as any);
};

afterEach(() => {
    delete (globalThis as any).fetch;
    vi.clearAllMocks();
});

describe('useDashboard', () => {
    it('fetches /api/dashboard and returns upcomingRegistrationsHref when link is present', async () => {
        const href = '/api/events?registeredBy=me&dateFrom=2026-04-24&sort=eventDate,ASC&size=3';
        mockFetchWithResponse({
            _links: {
                self: {href: '/api/dashboard'},
                upcomingRegistrations: {href},
            },
        });

        const {result} = renderHook(() => useDashboard(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data?.upcomingRegistrationsHref).toBe(href);
    });

    it('returns upcomingRegistrationsHref as undefined when link is absent', async () => {
        mockFetchWithResponse({
            _links: {
                self: {href: '/api/dashboard'},
            },
        });

        const {result} = renderHook(() => useDashboard(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data?.upcomingRegistrationsHref).toBeUndefined();
    });

    it('returns loading state initially', () => {
        (globalThis as any).fetch = vi.fn().mockReturnValue(new Promise(() => {}));

        const {result} = renderHook(() => useDashboard(), {wrapper: createWrapper()});

        expect(result.current.isLoading).toBe(true);
    });

    it('returns error state when fetch fails', async () => {
        (globalThis as any).fetch = vi.fn().mockRejectedValue(new Error('Network error'));

        // retryDelay: 0 ensures the single retry from useDashboard completes without delay
        const queryClient = new QueryClient({
            defaultOptions: {queries: {retryDelay: 0, gcTime: 0}},
        });
        const wrapper = ({children}: {children: ReactNode}) =>
            React.createElement(QueryClientProvider, {client: queryClient}, children);

        const {result} = renderHook(() => useDashboard(), {wrapper});

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toBeDefined();
    });
});
