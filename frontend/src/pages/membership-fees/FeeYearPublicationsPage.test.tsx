import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {FeeYearPublicationsPage} from './FeeYearPublicationsPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../contexts/halFormContext.ts', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
}));

const createMockPageData = (resourceData: HalResponse | null, overrides?: Record<string, unknown>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/administration/fee-year-publications',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/fee-year-publications'}),
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

const renderPage = (pageData: ReturnType<typeof createMockPageData>) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData as ReturnType<typeof useHalPageData>);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/administration/fee-year-publications']}>
                <FeeYearPublicationsPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('FeeYearPublicationsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders page title "Vypsání pro rok"', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByRole('heading', {level: 1, name: 'Vypsání pro rok'})).toBeInTheDocument();
    });

    it('renders table column headers', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByText('Rok')).toBeInTheDocument();
        expect(screen.getByText('Uzávěrka hlasování')).toBeInTheDocument();
    });

    it('shows skeleton loading state when data is loading', () => {
        renderPage(createMockPageData(null, {isLoading: true}));
        expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
    });

    it('shows error message when fetch fails', () => {
        renderPage(createMockPageData(null, {error: new Error('Chyba serveru')}));
        expect(screen.getByText('Chyba serveru')).toBeInTheDocument();
    });

    it('renders "Vypsat rok" button when publishYear template exists', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/fee-year-publications'}},
            _templates: {
                publishYear: mockHalFormsTemplate({title: 'Vypsat rok', method: 'POST'}),
            },
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /vypsat rok/i})).toBeInTheDocument();
    });

    it('does not render publish button when template is absent', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/fee-year-publications'}},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /vypsat rok/i})).not.toBeInTheDocument();
    });

    it('renders publication year in the table', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/fee-year-publications'}},
            _embedded: {
                feeYearPublicationResponseList: [{
                    id: 'pub-1',
                    year: 2025,
                    votingDeadline: '2025-03-31',
                    _links: {self: {href: '/api/fee-year-publications/pub-1'}},
                }],
            },
            page: {size: 10, totalElements: 1, totalPages: 1, number: 0},
        };
        vi.mocked(useAuthorizedQuery).mockReturnValue({data: resourceData, error: null} as ReturnType<typeof useAuthorizedQuery>);
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('2025')).toBeInTheDocument();
    });
});
