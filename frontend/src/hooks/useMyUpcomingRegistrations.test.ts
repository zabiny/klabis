import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import React from 'react';
import {useMyUpcomingRegistrations} from './useMyUpcomingRegistrations';
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

const buildEventsPageResponse = (events: { id: string; name: string; eventDate: string; location: string }[]) => ({
    _embedded: {
        eventSummaryDtoList: events.map(e => ({
            id: {value: e.id},
            name: e.name,
            eventDate: e.eventDate,
            location: e.location,
            _links: {self: {href: `/api/events/${e.id}`}},
        })),
    },
    _links: {self: {href: '/api/events?registeredBy=me&dateFrom=2026-04-24&sort=eventDate,ASC&size=3'}},
    page: {size: 3, totalElements: events.length, totalPages: 1, number: 0},
});

describe('useMyUpcomingRegistrations', () => {
    it('fetches events when href is provided and returns event items', async () => {
        const href = '/api/events?registeredBy=me&dateFrom=2026-04-24&sort=eventDate,ASC&size=3';
        mockFetchWithResponse(buildEventsPageResponse([
            {id: 'event-1', name: 'Jarní závod', eventDate: '2026-05-01', location: 'Praha'},
            {id: 'event-2', name: 'Letní kemp', eventDate: '2026-06-15', location: 'Brno'},
        ]));

        const {result} = renderHook(() => useMyUpcomingRegistrations(href), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data?.items ?? [];
        expect(items).toHaveLength(2);
        expect(items[0].name).toBe('Jarní závod');
        expect(items[0].eventDate).toBe('2026-05-01');
        expect(items[0].location).toBe('Praha');
        expect(items[0].selfHref).toBe('/api/events/event-1');
    });

    it('does not fetch when href is undefined (query is disabled)', () => {
        (globalThis as any).fetch = vi.fn();

        const {result} = renderHook(() => useMyUpcomingRegistrations(undefined), {wrapper: createWrapper()});

        expect(result.current.fetchStatus).toBe('idle');
        expect((globalThis as any).fetch).not.toHaveBeenCalled();
    });

    it('returns empty items array when response has no embedded events', async () => {
        const href = '/api/events?registeredBy=me&dateFrom=2026-04-24&sort=eventDate,ASC&size=3';
        mockFetchWithResponse({
            _embedded: {eventSummaryDtoList: []},
            _links: {self: {href}},
            page: {size: 3, totalElements: 0, totalPages: 0, number: 0},
        });

        const {result} = renderHook(() => useMyUpcomingRegistrations(href), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data?.items).toHaveLength(0);
    });

    it('returns loading state while fetching', () => {
        const href = '/api/events?registeredBy=me&dateFrom=2026-04-24&sort=eventDate,ASC&size=3';
        (globalThis as any).fetch = vi.fn().mockReturnValue(new Promise(() => {}));

        const {result} = renderHook(() => useMyUpcomingRegistrations(href), {wrapper: createWrapper()});

        expect(result.current.isLoading).toBe(true);
    });
});
