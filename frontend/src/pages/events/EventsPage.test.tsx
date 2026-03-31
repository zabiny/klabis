import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {EventsPage} from './EventsPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
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

const renderPage = (pageData: any) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/events']}>
                <EventsPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
};

describe('EventsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
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
});
