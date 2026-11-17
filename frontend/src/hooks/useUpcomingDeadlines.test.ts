import {renderHook} from '@testing-library/react';
import {vi} from 'vitest';
import {type UpcomingDeadlineItem, useUpcomingDeadlines} from './useUpcomingDeadlines';

vi.mock('./useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

import {useAuthorizedQuery} from './useAuthorizedFetch';

const mockUseAuthorizedQuery = vi.mocked(useAuthorizedQuery);

const HREF = '/api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc';

interface EventFixture {
    id?: string;
    name?: string;
    eventDate?: string;
    deadlines?: string[];
    _links: {self?: {href: string}; newRegistration?: {href: string}};
}

const eventRow = (overrides: Partial<EventFixture> = {}): EventFixture => ({
    id: 'evt-1',
    name: 'Jarní závod',
    eventDate: '2026-05-20',
    deadlines: ['2026-05-14'],
    _links: {self: {href: '/api/events/evt-1'}},
    ...overrides,
});

const respondWithEvents = (events: EventFixture[], totalElements?: number) => {
    mockUseAuthorizedQuery.mockReturnValue({
        data: {
            _links: {self: {href: HREF}},
            _embedded: {eventSummaryDtoList: events},
            ...(totalElements === undefined ? {} : {page: {size: 5, totalElements, totalPages: 1, number: 0}}),
        },
        isLoading: false,
        error: null,
        refetch: vi.fn(),
    } as unknown as ReturnType<typeof useAuthorizedQuery>);
};

const item = (result: { current: { items: UpcomingDeadlineItem[] } }, index: number) => result.current.items[index];

describe('useUpcomingDeadlines', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('projects a captured event row to an upcoming deadline item', () => {
        respondWithEvents([
            eventRow({
                deadlines: ['2026-05-14'],
                _links: {
                    self: {href: '/api/events/evt-1'},
                    newRegistration: {href: '/api/events/evt-1/registrations/new'},
                },
            }),
            eventRow({id: 'evt-2', name: 'Letní kemp', eventDate: '2026-06-10', deadlines: ['2026-05-15']}),
        ], 2);

        const {result} = renderHook(() => useUpcomingDeadlines(HREF));

        expect(result.current.items).toHaveLength(2);
        expect(item(result, 0)).toEqual({
            selfHref: '/api/events/evt-1',
            name: 'Jarní závod',
            eventDate: '2026-05-20',
            deadline: '2026-05-14',
            newRegistration: {href: '/api/events/evt-1/registrations/new'},
        });
        expect(item(result, 1).newRegistration).toBeUndefined();
    });

    it('skips events without a self link', () => {
        respondWithEvents([
            eventRow({name: 'Bez odkazu', _links: {}}),
            eventRow({name: 'S odkazem'}),
        ], 2);

        const {result} = renderHook(() => useUpcomingDeadlines(HREF));

        expect(result.current.items.map(i => i.name)).toEqual(['S odkazem']);
    });

    it('picks the first deadline not in the past when an event has several', () => {
        const futureDate1 = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
        const futureDate2 = new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
        respondWithEvents([eventRow({deadlines: [futureDate1, futureDate2]})]);

        const {result} = renderHook(() => useUpcomingDeadlines(HREF));

        expect(item(result, 0).deadline).toBe(futureDate1);
    });

    it('falls back to the last deadline when every deadline is in the past', () => {
        respondWithEvents([eventRow({deadlines: ['2026-01-01', '2026-02-01']})]);

        const {result} = renderHook(() => useUpcomingDeadlines(HREF));

        expect(item(result, 0).deadline).toBe('2026-02-01');
    });

    it('exposes totalElements from the page metadata', () => {
        respondWithEvents([eventRow()], 12);

        const {result} = renderHook(() => useUpcomingDeadlines(HREF));

        expect(result.current.totalElements).toBe(12);
    });

    it('defaults totalElements to 0 without page metadata', () => {
        respondWithEvents([eventRow()]);

        const {result} = renderHook(() => useUpcomingDeadlines(HREF));

        expect(result.current.totalElements).toBe(0);
    });
});
