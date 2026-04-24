import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
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
