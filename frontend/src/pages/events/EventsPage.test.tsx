import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {EventsPage} from './EventsPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {useAuth} from '../../contexts/AuthContext2';
import {labels} from '../../localization';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/AuthContext2', () => ({
    useAuth: vi.fn().mockReturnValue({
        getUser: () => ({memberId: 'M001', firstName: 'Jana', lastName: 'Novak', id: 1, userName: 'ZBM9500'}),
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
        isLoading: false,
    }),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../components/events/ImportOrisEventModal', () => ({
    ImportOrisEventModal: ({isOpen, onClose}: {isOpen: boolean; onClose: () => void}) =>
        isOpen ? (
            <div data-testid="import-oris-modal">
                <span>Import akce z ORIS</span>
                <button onClick={onClose}>Zavřít</button>
            </div>
        ) : null,
}));

vi.mock('../../hooks/useOrisEventImport', () => ({
    useOrisEventImport: vi.fn().mockReturnValue({
        events: [],
        fetchState: 'loading',
        selectedRegion: 'JIHOMORAVSKA',
        isSubmitting: false,
        submitError: null,
        onRegionChange: vi.fn(),
        onImport: vi.fn(),
    }),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

vi.mock('../../api/authorizedFetch', () => ({
    authorizedFetch: vi.fn(),
    FetchError: class FetchError extends Error {
        public responseStatus: number;
        constructor(message: string, status: number) {
            super(message);
            this.responseStatus = status;
        }
    },
}));

const createMockPageData = (resourceData: HalResponse | null, overrides?: any) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/events',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/events'}),
    },
    actions: {handleNavigateToItem: vi.fn()},
    getLinks: vi.fn(() => undefined),
    getTemplates: vi.fn(() => undefined),
    hasEmbedded: vi.fn(() => false),
    getEmbeddedItems: vi.fn(() => []),
    isCollection: vi.fn(() => false),
    hasLink: vi.fn(() => false),
    hasTemplate: vi.fn(() => false),
    hasForms: vi.fn(() => false),
    getPageMetadata: vi.fn(() => undefined),
    ...overrides,
});

const renderPage = (pageData: any, initialPath = '/events') => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialPath]}>
                <EventsPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
};

describe('EventsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(useAuth).mockReturnValue({
            getUser: () => ({memberId: 'M001', firstName: 'Jana', lastName: 'Novak', id: 1, userName: 'ZBM9500'}),
            isAuthenticated: true,
            login: vi.fn(),
            logout: vi.fn(),
            isLoading: false,
        });
    });

    it('renders page title "Akce"', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByRole('heading', {name: 'Akce'})).toBeInTheDocument();
    });

    describe('deadlines column (6.4)', () => {
        beforeEach(() => {
            vi.useFakeTimers();
            vi.setSystemTime(new Date('2025-05-09'));
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        const buildEventRow = (overrides: Record<string, unknown> = {}) => ({
            id: 'evt-1',
            name: 'Jarní závod',
            eventDate: '2025-04-15',
            status: 'ACTIVE',
            _links: {self: {href: '/api/events/1'}},
            ...overrides,
        });

        const renderWithEvents = (rows: unknown[]) => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {
                    _links: {self: {href: '/api/events'}},
                    _embedded: {eventSummaryDtoList: rows},
                    page: {totalElements: rows.length, totalPages: 1, size: 10, number: 0},
                },
                isLoading: false,
                error: null,
            });
            return renderPage(createMockPageData({
                _links: {self: {href: '/api/events'}},
            }));
        };

        it('shows Uzávěrka column header', () => {
            renderWithEvents([buildEventRow()]);
            expect(screen.getByRole('columnheader', {name: /uzávěrka/i})).toBeInTheDocument();
        });

        it('shows formatted deadline from deadlines[0] when one deadline present', () => {
            renderWithEvents([buildEventRow({deadlines: ['2025-03-15']})]);
            expect(screen.getByText('15. 3. 2025')).toBeInTheDocument();
        });

        it('shows relevant deadline and badge when multiple deadlines present', () => {
            renderWithEvents([buildEventRow({deadlines: ['2025-03-01', '2025-04-01', '2025-04-15']})]);
            expect(screen.getByTitle(/1\. 4\. 2025|1\. 3\. 2025|15\. 4\. 2025/)).toBeInTheDocument();
        });

        it('shows first future deadline as relevant when first deadline has already passed', () => {
            renderWithEvents([buildEventRow({deadlines: ['2020-01-01', '2099-12-31']})]);
            expect(screen.getByText('31. 12. 2099')).toBeInTheDocument();
        });

        it('shows last deadline as relevant when all deadlines have passed', () => {
            renderWithEvents([buildEventRow({deadlines: ['2020-01-01', '2021-06-01']})]);
            expect(screen.getByText('1. 6. 2021')).toBeInTheDocument();
        });

        it('shows first deadline (index 0) for single deadline event', () => {
            renderWithEvents([buildEventRow({deadlines: ['2025-03-15']})]);
            expect(screen.getByText('15. 3. 2025')).toBeInTheDocument();
        });
    });

    describe('cancel event action modal', () => {
        const buildEventWithCancelTemplate = () => ({
            id: 'evt-cancel',
            name: 'Zrušitelná akce',
            eventDate: '2025-06-15',
            status: 'ACTIVE',
            deadlines: ['2025-05-01'],
            _links: {self: {href: '/api/events/cancel-evt'}},
            _templates: {
                cancelEvent: mockHalFormsTemplate({
                    method: 'POST',
                    target: '/api/events/cancel-evt/cancel',
                    title: undefined,
                    properties: [{name: 'cancellationReason', type: 'textarea', required: false}],
                }),
            },
        });

        const renderWithCancelableEvent = () => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {
                    _links: {self: {href: '/api/events'}},
                    _embedded: {eventSummaryDtoList: [buildEventWithCancelTemplate()]},
                    page: {totalElements: 1, totalPages: 1, size: 10, number: 0},
                },
                isLoading: false,
                error: null,
            });
            return renderPage(createMockPageData({
                _links: {self: {href: '/api/events'}},
            }));
        };

        it('shows localized dialog title when backend does not send template.title', async () => {
            const user = userEvent.setup();
            renderWithCancelableEvent();

            const cancelButton = await screen.findByTitle(labels.templates.cancelEvent);
            await user.click(cancelButton);

            expect(screen.getByTestId('modal-title')).toHaveTextContent('Zrušení akce');
        });
    });

    describe('"Importovat z ORIS" button', () => {
        it('shows button when importEvent template exists in HAL response', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {
                    importEvent: mockHalFormsTemplate({
                        method: 'POST',
                        target: '/api/events/import',
                        title: 'Importovat z ORIS',
                    }),
                },
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.getByRole('button', {name: /importovat z oris/i})).toBeInTheDocument();
        });

        it('does NOT show button when importEvent template is absent', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {
                    createEvent: mockHalFormsTemplate({method: 'POST', title: 'Přidat závod'}),
                },
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.queryByRole('button', {name: /importovat z oris/i})).not.toBeInTheDocument();
        });

        it('does NOT show button when no templates exist', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.queryByRole('button', {name: /importovat z oris/i})).not.toBeInTheDocument();
        });

        it('opens ImportOrisEventModal when button is clicked', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {
                    importEvent: mockHalFormsTemplate({
                        method: 'POST',
                        target: '/api/events/import',
                        title: 'Importovat z ORIS',
                    }),
                },
            };
            renderPage(createMockPageData(resourceData));

            await user.click(screen.getByRole('button', {name: /importovat z oris/i}));

            expect(screen.getByText('Import akce z ORIS')).toBeInTheDocument();
        });
    });

    describe('filter bar', () => {
        it('renders filter bar above the events table', () => {
            renderPage(createMockPageData(null));
            expect(screen.getByPlaceholderText(labels.eventsFilter.searchPlaceholder)).toBeInTheDocument();
            expect(screen.getByRole('button', {name: labels.eventsFilter.budouci})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: labels.eventsFilter.probehle})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: labels.eventsFilter.vse})).toBeInTheDocument();
        });

        it('shows "Moje přihlášky" checkbox for user with member profile', () => {
            renderPage(createMockPageData(null));
            expect(screen.getByRole('checkbox', {name: labels.eventsFilter.mojePřihlaskyLabel})).toBeInTheDocument();
        });

        it('hides "Moje přihlášky" checkbox for user without member profile', () => {
            vi.mocked(useAuth).mockReturnValue({
                getUser: () => ({memberId: null, firstName: 'Admin', lastName: 'User', id: 2, userName: 'ZBM9000'}),
                isAuthenticated: true,
                login: vi.fn(),
                logout: vi.fn(),
                isLoading: false,
            });
            renderPage(createMockPageData(null));
            expect(screen.queryByRole('checkbox', {name: labels.eventsFilter.mojePřihlaskyLabel})).not.toBeInTheDocument();
        });

        it('shows Budoucí as active time window by default', () => {
            renderPage(createMockPageData(null));
            const budouciBtn = screen.getByRole('button', {name: labels.eventsFilter.budouci});
            expect(budouciBtn).toHaveAttribute('aria-pressed', 'true');
        });

        it('shows Proběhlé as active when dateTo param is present in URL', () => {
            renderPage(createMockPageData(null), '/events?dateTo=2024-06-14');
            const probehleBtn = screen.getByRole('button', {name: labels.eventsFilter.probehle});
            expect(probehleBtn).toHaveAttribute('aria-pressed', 'true');
        });

        it('shows Vše as active when both dateFrom and dateTo are present (edge case)', () => {
            renderPage(createMockPageData(null), '/events?dateFrom=2024-01-01&dateTo=2024-12-31');
            const vseBtn = screen.getByRole('button', {name: labels.eventsFilter.vse});
            expect(vseBtn).toHaveAttribute('aria-pressed', 'true');
        });

        it('switches time window to Vše when Vše button is clicked', async () => {
            const user = userEvent.setup();
            renderPage(createMockPageData(null));

            await user.click(screen.getByRole('button', {name: labels.eventsFilter.vse}));

            await waitFor(() => {
                const vseBtn = screen.getByRole('button', {name: labels.eventsFilter.vse});
                expect(vseBtn).toHaveAttribute('aria-pressed', 'true');
            });
        });

        it('reflects search query from URL', () => {
            renderPage(createMockPageData(null), '/events?q=jihlava');
            expect(screen.getByDisplayValue('jihlava')).toBeInTheDocument();
        });

        it('reflects registeredBy=me from URL as checked checkbox', () => {
            renderPage(createMockPageData(null), '/events?registeredBy=me');
            const checkbox = screen.getByRole('checkbox', {name: labels.eventsFilter.mojePřihlaskyLabel});
            expect(checkbox).toBeChecked();
        });
    });
});
