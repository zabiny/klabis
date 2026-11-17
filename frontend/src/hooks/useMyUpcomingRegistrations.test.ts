import {renderHook} from '@testing-library/react';
import {vi} from 'vitest';
import {useMyUpcomingRegistrations} from './useMyUpcomingRegistrations';

vi.mock('./useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

import {useAuthorizedQuery} from './useAuthorizedFetch';

const mockUseAuthorizedQuery = vi.mocked(useAuthorizedQuery);

const HREF = '/api/events?registeredBy=me&dateFrom=2026-04-24&sort=eventDate,ASC&size=3';

interface EventFixture {
    name?: string;
    eventDate?: string;
    location?: string;
    _links: {self?: {href: string}};
    _templates?: {editRegistration?: {target?: string}};
}

const respondWithEvents = (events: EventFixture[]) => {
    mockUseAuthorizedQuery.mockReturnValue({
        data: {
            _links: {self: {href: HREF}},
            _embedded: {eventSummaryDtoList: events},
        },
        isLoading: false,
        error: null,
        refetch: vi.fn(),
    } as unknown as ReturnType<typeof useAuthorizedQuery>);
};

const eventRow = (overrides: Partial<EventFixture> = {}): EventFixture => ({
    name: 'Jarní závod',
    eventDate: '2026-05-01',
    location: 'Praha',
    _links: {self: {href: '/api/events/event-1'}},
    ...overrides,
});

describe('useMyUpcomingRegistrations', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('projects a captured event row to an upcoming registration item', () => {
        respondWithEvents([
            eventRow(),
            eventRow({name: 'Letní kemp', eventDate: '2026-06-15', location: 'Brno', _links: {self: {href: '/api/events/event-2'}}}),
        ]);

        const {result} = renderHook(() => useMyUpcomingRegistrations(HREF));

        expect(result.current.items).toEqual([
            {selfHref: '/api/events/event-1', name: 'Jarní závod', eventDate: '2026-05-01', location: 'Praha', editRegistrationTarget: undefined},
            {selfHref: '/api/events/event-2', name: 'Letní kemp', eventDate: '2026-06-15', location: 'Brno', editRegistrationTarget: undefined},
        ]);
    });

    it('skips events without a self link', () => {
        respondWithEvents([
            eventRow({name: 'Bez odkazu', _links: {}}),
            eventRow({name: 'S odkazem'}),
        ]);

        const {result} = renderHook(() => useMyUpcomingRegistrations(HREF));

        expect(result.current.items.map(i => i.name)).toEqual(['S odkazem']);
    });

    it('exposes the editRegistration template target when the event carries it', () => {
        respondWithEvents([
            eventRow({_templates: {editRegistration: {target: '/api/events/event-1/registrations/me'}}}),
            eventRow({name: 'Letní kemp', _links: {self: {href: '/api/events/event-2'}}}),
        ]);

        const {result} = renderHook(() => useMyUpcomingRegistrations(HREF));

        expect(result.current.items[0].editRegistrationTarget).toBe('/api/events/event-1/registrations/me');
        expect(result.current.items[1].editRegistrationTarget).toBeUndefined();
    });
});
