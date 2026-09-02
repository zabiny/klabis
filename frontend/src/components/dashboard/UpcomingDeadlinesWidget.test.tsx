import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider, type UseQueryResult} from '@tanstack/react-query';
import {vi} from 'vitest';
import React, {type ComponentProps} from 'react';
import {UpcomingDeadlinesWidget} from './UpcomingDeadlinesWidget';
import * as UseUpcomingDeadlinesModule from '../../hooks/useUpcomingDeadlines';
import {type UpcomingDeadlineItem, type UpcomingDeadlinesData} from '../../hooks/useUpcomingDeadlines';
import type {EventRegistrationDialog as EventRegistrationDialogComponent} from '../events/EventRegistrationDialog';

type EventRegistrationDialogProps = ComponentProps<typeof EventRegistrationDialogComponent>;

vi.mock('../../hooks/useUpcomingDeadlines', () => ({
    useUpcomingDeadlines: vi.fn(),
}));

vi.mock('../events/EventRegistrationDialog', () => ({
    EventRegistrationDialog: (props: EventRegistrationDialogProps) => props.isOpen ? React.createElement('div', {
        'data-testid': 'event-registration-dialog',
        'data-mode': props.mode,
        'data-template-target': props.template?.target ?? '',
        'data-event-name': props.event?.name ?? '',
        'data-prefill-href': props.prefillHref ?? '',
        'data-location': props.event?.location ?? '',
        'data-deadlines': (props.event?.deadlines ?? []).join(','),
        'data-shared-transport-enabled': String(props.event?.sharedTransportEnabled),
        'data-shared-accommodation-enabled': String(props.event?.sharedAccommodationEnabled),
    }, [
        React.createElement('button', {key: 'close', onClick: props.onClose}, 'dialog-close'),
        React.createElement('button', {key: 'register', onClick: props.onRegistered}, 'dialog-registered'),
    ]) : null,
}));

const useUpcomingDeadlines = vi.mocked(UseUpcomingDeadlinesModule.useUpcomingDeadlines);

function createMockQueryResult(data?: UpcomingDeadlinesData | null): UseQueryResult<UpcomingDeadlinesData | undefined>;
function createMockQueryResult<T>(data?: T | null): UseQueryResult<T | undefined>;
function createMockQueryResult<T>(data: T | null = null): UseQueryResult<T | undefined> {
    return {
        data: data ?? undefined,
        isLoading: false,
        isError: false,
        isPending: false,
        error: null,
        status: 'success' as const,
        fetchStatus: 'idle' as const,
        isFetched: true,
        isStale: false,
        isFetching: false,
        isPlaceholderData: false,
        isRefetching: false,
        refetch: vi.fn(),
        failureCount: 0,
        failureReason: null,
        errorUpdateCount: 0,
        errorUpdatedAt: null,
        dataUpdatedAt: Date.now(),
    } as unknown as UseQueryResult<T | undefined>;
}

const renderWidget = (href: string | undefined = '/api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc') => {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false, gcTime: 0}},
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <UpcomingDeadlinesWidget upcomingDeadlinesHref={href}/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const makeItem = (overrides: Partial<UpcomingDeadlineItem> = {}): UpcomingDeadlineItem => ({
    selfHref: '/api/events/evt-1',
    name: 'Test akce',
    eventDate: '2026-06-01',
    location: undefined,
    deadlines: ['2026-05-14'],
    deadline: '2026-05-14',
    sharedTransportEnabled: undefined,
    sharedAccommodationEnabled: undefined,
    newRegistrationHref: '/api/events/evt-1/registrations/new',
    registerForEventTemplate: {
        target: '/api/events/evt-1/registrations',
        method: 'POST',
        properties: [],
    },
    ...overrides,
});

const makeItems = (count: number): UpcomingDeadlineItem[] =>
    Array.from({length: count}, (_, i) => makeItem({
        selfHref: `/api/events/evt-${i + 1}`,
        name: `Závod ${i + 1}`,
        eventDate: `2026-05-${String(20 + i).padStart(2, '0')}`,
        deadlines: [`2026-05-${String(14 + i).padStart(2, '0')}`],
        deadline: `2026-05-${String(14 + i).padStart(2, '0')}`,
        newRegistrationHref: `/api/events/evt-${i + 1}/registrations/new`,
        registerForEventTemplate: {
            target: `/api/events/evt-${i + 1}/registrations`,
            method: 'POST',
            properties: [],
        },
    }));

const makeMockResult = (items: UpcomingDeadlineItem[], totalElements?: number) =>
    createMockQueryResult({items, totalElements: totalElements ?? items.length});

describe('UpcomingDeadlinesWidget', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('with 5 events (totalElements = 5, no footer link expected)', () => {
        beforeEach(() => {
            useUpcomingDeadlines.mockReturnValue(makeMockResult(makeItems(5), 5));
        });

        it('renders the widget title', () => {
            renderWidget();
            expect(screen.getByText('Končící přihlášky tento týden')).toBeInTheDocument();
        });

        it('renders all 5 event names', () => {
            renderWidget();
            for (let i = 1; i <= 5; i++) {
                expect(screen.getByText(`Závod ${i}`)).toBeInTheDocument();
            }
        });

        it('renders formatted event dates', () => {
            renderWidget();
            expect(screen.getByText('20. 5. 2026')).toBeInTheDocument();
        });

        it('renders deadline with prefix for each event', () => {
            renderWidget();
            const deadlineTexts = screen.getAllByText(/Uzávěrka:/);
            expect(deadlineTexts.length).toBeGreaterThanOrEqual(1);
        });

        it('renders "Přihlásit se" buttons for each event', () => {
            renderWidget();
            const buttons = screen.getAllByRole('button', {name: /Přihlásit se/i});
            expect(buttons).toHaveLength(5);
        });
    });

    describe('with 5 events shown but totalElements = 10 (more than displayed)', () => {
        beforeEach(() => {
            useUpcomingDeadlines.mockReturnValue(makeMockResult(makeItems(5), 10));
        });

        it('renders footer "Zobrazit všechny s končící uzávěrkou" link', () => {
            renderWidget();
            expect(screen.getByRole('link', {name: /Zobrazit všechny s končící uzávěrkou/i})).toBeInTheDocument();
        });

        it('footer link points to events list with deadlineWithin and notRegisteredBy filters', () => {
            renderWidget();
            const link = screen.getByRole('link', {name: /Zobrazit všechny s končící uzávěrkou/i});
            expect(link).toHaveAttribute('href', expect.stringContaining('deadlineWithin=P7D'));
            expect(link).toHaveAttribute('href', expect.stringContaining('notRegisteredBy=me'));
        });
    });

    describe('with 2 events', () => {
        beforeEach(() => {
            useUpcomingDeadlines.mockReturnValue(makeMockResult(makeItems(2), 2));
        });

        it('renders widget with 2 events', () => {
            renderWidget();
            expect(screen.getByText('Závod 1')).toBeInTheDocument();
            expect(screen.getByText('Závod 2')).toBeInTheDocument();
            expect(screen.queryByText('Závod 3')).not.toBeInTheDocument();
        });

        it('renders 2 "Přihlásit se" buttons', () => {
            renderWidget();
            expect(screen.getAllByRole('button', {name: /Přihlásit se/i})).toHaveLength(2);
        });

        it('does not render footer link when totalElements <= displayed count', () => {
            renderWidget();
            expect(screen.queryByRole('link', {name: /Zobrazit všechny s končící uzávěrkou/i})).not.toBeInTheDocument();
        });
    });

    describe('with 0 events', () => {
        beforeEach(() => {
            useUpcomingDeadlines.mockReturnValue(makeMockResult([], 0));
        });

        it('does not render the widget at all', () => {
            renderWidget();
            expect(screen.queryByText('Končící přihlášky tento týden')).not.toBeInTheDocument();
        });
    });

    describe('when href is undefined (no member profile)', () => {
        beforeEach(() => {
            useUpcomingDeadlines.mockReturnValue(createMockQueryResult(undefined));
        });

        it('does not render the widget', () => {
            renderWidget(undefined);
            expect(screen.queryByText('Končící přihlášky tento týden')).not.toBeInTheDocument();
        });
    });

    describe('a11y: row navigation', () => {
        beforeEach(() => {
            useUpcomingDeadlines.mockReturnValue(makeMockResult([
                makeItem({newRegistrationHref: undefined, registerForEventTemplate: undefined}),
            ], 1));
        });

        it('row is a RouterLink (has href attribute) for keyboard accessibility', () => {
            renderWidget();
            const links = screen.getAllByRole('link');
            const eventLink = links.find(l => l.getAttribute('href')?.includes('/events/evt-1'));
            expect(eventLink).toBeDefined();
        });
    });

    describe('deadline format', () => {
        it('formats deadline as "DD. MM." with Uzávěrka: prefix', () => {
            useUpcomingDeadlines.mockReturnValue(createMockQueryResult({
                items: [makeItem({deadline: '2026-05-14'})],
                totalElements: 1,
            }));
            renderWidget();
            expect(screen.getByText(/Uzávěrka:/)).toBeInTheDocument();
            expect(screen.getByText(/14\. 5\. 2026/)).toBeInTheDocument();
        });
    });

    describe('"Přihlásit se" opens the customized registration dialog', () => {
        it('opens EventRegistrationDialog in new mode with row template, prefill href and event context', () => {
            useUpcomingDeadlines.mockReturnValue(createMockQueryResult({
                items: [makeItem({
                    name: 'Test akce',
                    newRegistrationHref: '/api/events/evt-1/registrations/new',
                    registerForEventTemplate: {
                        target: '/api/events/evt-1/registrations',
                        method: 'POST' as const,
                        properties: [],
                    },
                })],
                totalElements: 1,
            }));
            renderWidget();

            fireEvent.click(screen.getByRole('button', {name: /Přihlásit se/i}));

            const dialog = screen.getByTestId('event-registration-dialog');
            expect(dialog).toBeInTheDocument();
            expect(dialog).toHaveAttribute('data-mode', 'new');
            expect(dialog).toHaveAttribute('data-template-target', '/api/events/evt-1/registrations');
            expect(dialog).toHaveAttribute('data-prefill-href', '/api/events/evt-1/registrations/new');
            expect(dialog).toHaveAttribute('data-event-name', 'Test akce');
        });

        it('does not navigate away from the dashboard when the dialog opens', () => {
            useUpcomingDeadlines.mockReturnValue(makeMockResult(makeItems(1), 1));
            renderWidget();

            fireEvent.click(screen.getByRole('button', {name: /Přihlásit se/i}));

            expect(screen.getByTestId('event-registration-dialog')).toBeInTheDocument();
            expect(screen.getByText('Končící přihlášky tento týden')).toBeInTheDocument();
        });

        it('passes shared-offer flags and event context from the row to the dialog', () => {
            useUpcomingDeadlines.mockReturnValue(createMockQueryResult({
                items: [makeItem({
                    location: 'Třebíč',
                    deadlines: ['2026-05-14', '2026-05-20'],
                    sharedTransportEnabled: true,
                    sharedAccommodationEnabled: false,
                })],
                totalElements: 1,
            }));
            renderWidget();

            fireEvent.click(screen.getByRole('button', {name: /Přihlásit se/i}));

            const dialog = screen.getByTestId('event-registration-dialog');
            expect(dialog).toHaveAttribute('data-shared-transport-enabled', 'true');
            expect(dialog).toHaveAttribute('data-shared-accommodation-enabled', 'false');
            expect(dialog).toHaveAttribute('data-location', 'Třebíč');
            expect(dialog).toHaveAttribute('data-deadlines', '2026-05-14,2026-05-20');
        });

        it('closes the dialog on close', () => {
            useUpcomingDeadlines.mockReturnValue(makeMockResult(makeItems(1), 1));
            renderWidget();

            fireEvent.click(screen.getByRole('button', {name: /Přihlásit se/i}));
            expect(screen.getByTestId('event-registration-dialog')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'dialog-close'}));
            expect(screen.queryByTestId('event-registration-dialog')).not.toBeInTheDocument();
        });

        it('refetches the deadlines query after a successful registration', () => {
            const refetch = vi.fn();
            useUpcomingDeadlines.mockReturnValue({...makeMockResult(makeItems(1), 1), refetch});
            renderWidget();

            fireEvent.click(screen.getByRole('button', {name: /Přihlásit se/i}));
            fireEvent.click(screen.getByRole('button', {name: 'dialog-registered'}));

            expect(refetch).toHaveBeenCalled();
        });
    });
});
