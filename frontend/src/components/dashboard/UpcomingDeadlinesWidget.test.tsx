import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import React from 'react';
import {UpcomingDeadlinesWidget} from './UpcomingDeadlinesWidget';
import * as UseUpcomingDeadlinesModule from '../../hooks/useUpcomingDeadlines';
import type {UpcomingDeadlineItem} from '../../hooks/useUpcomingDeadlines';
import * as UseAuthorizedFetchModule from '../../hooks/useAuthorizedFetch';

vi.mock('../../hooks/useUpcomingDeadlines', () => ({
    useUpcomingDeadlines: vi.fn(),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn(),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

vi.mock('../../components/HalNavigator2/HalFormDisplay', () => ({
    HalFormDisplay: () => React.createElement('div', {'data-testid': 'hal-form-display'}, 'Form'),
}));

vi.mock('../../components/UI', async () => {
    const actual = await vi.importActual('../../components/UI');
    return {
        ...actual,
        Modal: ({isOpen, children, title}: {isOpen: boolean; children: React.ReactNode; title: string; onClose: () => void}) =>
            isOpen ? React.createElement('div', {'data-testid': 'modal', 'aria-label': title}, children) : null,
    };
});

const useUpcomingDeadlines = vi.mocked(UseUpcomingDeadlinesModule.useUpcomingDeadlines);
const useAuthorizedQuery = vi.mocked(UseAuthorizedFetchModule.useAuthorizedQuery);

const createMockQueryResult = (data: any = null, overrides: any = {}) => ({
    data,
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
    ...overrides,
} as any);

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

const makeItems = (count: number): UpcomingDeadlineItem[] =>
    Array.from({length: count}, (_, i) => ({
        selfHref: `/api/events/evt-${i + 1}`,
        name: `Závod ${i + 1}`,
        eventDate: `2026-05-${String(20 + i).padStart(2, '0')}`,
        deadline: `2026-05-${String(14 + i).padStart(2, '0')}`,
        newRegistrationHref: `/api/events/evt-${i + 1}/registrations/new`,
        registerForEventTemplate: {
            target: `/api/events/evt-${i + 1}/registrations`,
            method: 'POST',
            properties: [],
        },
    }));

const makeMockResult = (items: ReturnType<typeof makeItems>, totalElements?: number) =>
    createMockQueryResult({items, totalElements: totalElements ?? items.length});

describe('UpcomingDeadlinesWidget', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        useAuthorizedQuery.mockReturnValue(createMockQueryResult(null));
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
                {
                    selfHref: '/api/events/evt-1',
                    name: 'Test akce',
                    eventDate: '2026-06-01',
                    deadline: '2026-05-14',
                    newRegistrationHref: undefined,
                    registerForEventTemplate: undefined,
                },
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
                items: [{
                    selfHref: '/api/events/evt-1',
                    name: 'Test akce',
                    eventDate: '2026-06-01',
                    deadline: '2026-05-14',
                    newRegistrationHref: '/api/events/evt-1/registrations/new',
                }],
            }));
            renderWidget();
            expect(screen.getByText(/Uzávěrka:/)).toBeInTheDocument();
            expect(screen.getByText(/14\. 5\. 2026/)).toBeInTheDocument();
        });
    });

    describe('"Přihlásit se" button interaction', () => {
        it('opens registration modal when "Přihlásit se" clicked and newRegistrationHref is present', async () => {
            useUpcomingDeadlines.mockReturnValue(createMockQueryResult({
                items: [{
                    selfHref: '/api/events/evt-1',
                    name: 'Test akce',
                    eventDate: '2026-06-01',
                    deadline: '2026-05-14',
                    newRegistrationHref: '/api/events/evt-1/registrations/new',
                    registerForEventTemplate: {
                        target: '/api/events/evt-1/registrations',
                        method: 'POST',
                        properties: [],
                    },
                }],
            }));

            useAuthorizedQuery.mockReturnValue(createMockQueryResult({
                _links: {},
                _templates: {
                    registerForEvent: {
                        target: '/api/events/evt-1/registrations',
                        method: 'POST',
                        properties: [],
                    },
                },
            }));

            renderWidget();

            const button = screen.getByRole('button', {name: /Přihlásit se/i});
            fireEvent.click(button);

            expect(screen.getByTestId('modal')).toBeInTheDocument();
        });
    });
});
