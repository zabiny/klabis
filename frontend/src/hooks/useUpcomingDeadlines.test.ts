import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import React from 'react';
import {useUpcomingDeadlines} from './useUpcomingDeadlines';
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

const buildEventsResponse = (events: {
    id: string;
    name: string;
    eventDate: string;
    deadlines: string[];
    newRegistrationHref?: string;
    registerForEventTarget?: string;
}[]) => ({
    _embedded: {
        eventSummaryDtoList: events.map(e => ({
            id: {value: e.id},
            name: e.name,
            eventDate: e.eventDate,
            deadlines: e.deadlines,
            _links: {
                self: {href: `/api/events/${e.id}`},
                ...(e.newRegistrationHref ? {newRegistration: {href: e.newRegistrationHref}} : {}),
            },
            _templates: e.registerForEventTarget
                ? {registerForEvent: {target: e.registerForEventTarget, method: 'POST', properties: []}}
                : undefined,
        })),
    },
    _links: {self: {href: '/api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc'}},
    page: {size: 5, totalElements: events.length, totalPages: 1, number: 0},
});

describe('useUpcomingDeadlines', () => {
    it('fetches events when href is provided and maps to UpcomingDeadlineItem', async () => {
        const href = '/api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc';
        mockFetchWithResponse(buildEventsResponse([
            {
                id: 'evt-1',
                name: 'Jarní závod',
                eventDate: '2026-05-20',
                deadlines: ['2026-05-14'],
                newRegistrationHref: '/api/events/evt-1/registrations/new',
                registerForEventTarget: '/api/events/evt-1/registrations',
            },
            {
                id: 'evt-2',
                name: 'Letní kemp',
                eventDate: '2026-06-10',
                deadlines: ['2026-05-15'],
                newRegistrationHref: '/api/events/evt-2/registrations/new',
            },
        ]));

        const {result} = renderHook(() => useUpcomingDeadlines(href), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data?.items ?? [];
        expect(items).toHaveLength(2);
        expect(items[0].name).toBe('Jarní závod');
        expect(items[0].eventDate).toBe('2026-05-20');
        expect(items[0].selfHref).toBe('/api/events/evt-1');
        expect(items[0].deadline).toBe('2026-05-14');
        expect(items[0].newRegistrationHref).toBe('/api/events/evt-1/registrations/new');
    });

    it('does not fetch when href is undefined (query is disabled)', () => {
        (globalThis as any).fetch = vi.fn();

        const {result} = renderHook(() => useUpcomingDeadlines(undefined), {wrapper: createWrapper()});

        expect(result.current.fetchStatus).toBe('idle');
        expect((globalThis as any).fetch).not.toHaveBeenCalled();
    });

    it('returns empty items array when response has no embedded events', async () => {
        const href = '/api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc';
        mockFetchWithResponse({
            _embedded: {eventSummaryDtoList: []},
            _links: {self: {href}},
            page: {size: 5, totalElements: 0, totalPages: 0, number: 0},
        });

        const {result} = renderHook(() => useUpcomingDeadlines(href), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data?.items).toHaveLength(0);
    });

    it('picks the first deadline >= today when event has multiple deadlines', async () => {
        const href = '/api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc';
        const today = new Date().toISOString().slice(0, 10);
        const futureDate1 = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
        const futureDate2 = new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
        mockFetchWithResponse(buildEventsResponse([
            {
                id: 'evt-1',
                name: 'Multi-deadline event',
                eventDate: '2026-06-01',
                deadlines: [futureDate1, futureDate2],
            },
        ]));

        const {result} = renderHook(() => useUpcomingDeadlines(href), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data?.items[0].deadline).toBe(futureDate1);
        void today;
    });

    it('falls back to last deadline when all deadlines are in the past', async () => {
        const href = '/api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc';
        const pastDate1 = '2026-01-01';
        const pastDate2 = '2026-02-01';
        mockFetchWithResponse(buildEventsResponse([
            {
                id: 'evt-1',
                name: 'Past-deadline event',
                eventDate: '2026-06-01',
                deadlines: [pastDate1, pastDate2],
            },
        ]));

        const {result} = renderHook(() => useUpcomingDeadlines(href), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data?.items[0].deadline).toBe(pastDate2);
    });

    it('extracts totalElements from page metadata', async () => {
        const href = '/api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc';
        const response = buildEventsResponse([
            {id: 'evt-1', name: 'Akce', eventDate: '2026-05-20', deadlines: ['2026-05-14']},
        ]);
        (response as any).page = {size: 5, totalElements: 12, totalPages: 3, number: 0};
        mockFetchWithResponse(response);

        const {result} = renderHook(() => useUpcomingDeadlines(href), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data?.totalElements).toBe(12);
    });

    it('returns loading state while fetching', () => {
        const href = '/api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc';
        (globalThis as any).fetch = vi.fn().mockReturnValue(new Promise(() => {}));

        const {result} = renderHook(() => useUpcomingDeadlines(href), {wrapper: createWrapper()});

        expect(result.current.isLoading).toBe(true);
    });
});
