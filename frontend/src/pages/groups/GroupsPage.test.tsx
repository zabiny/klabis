import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {GroupsPage} from './GroupsPage';
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

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

const createMockPageData = (resourceData: HalResponse | null, overrides?: Record<string, unknown>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/groups',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/groups'}),
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
            <MemoryRouter initialEntries={['/groups']}>
                <GroupsPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const buildGroupRow = (overrides?: Record<string, unknown>) => ({
    id: 'group-1',
    name: 'Testovací skupina',
    _links: {self: {href: '/api/groups/group-1'}},
    ...overrides,
});

const renderPageWithGroups = (groups: unknown[], routeOverrides?: Record<string, unknown>) => {
    const resourceData: HalResponse = {
        _links: {self: {href: 'http://localhost/api/groups'}},
        _embedded: {groupSummaryResponseList: groups},
        page: {size: 10, totalElements: groups.length, totalPages: 1, number: 0},
    };
    vi.mocked(useAuthorizedQuery).mockReturnValue({data: resourceData, error: null} as ReturnType<typeof useAuthorizedQuery>);
    const pageData = createMockPageData(resourceData, routeOverrides);
    return renderPage(pageData);
};

describe('GroupsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders page title "Skupiny"', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByText('Skupiny')).toBeInTheDocument();
    });

    it('renders section heading "Moje skupiny"', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByText('Moje skupiny')).toBeInTheDocument();
    });

    it('renders "Název" column header', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByText('Název')).toBeInTheDocument();
    });

    it('shows skeleton loading state when data is loading', () => {
        renderPage(createMockPageData(null, {isLoading: true}));
        expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
    });

    it('shows error message when fetch fails', () => {
        renderPage(createMockPageData(null, {error: new Error('Chyba serveru')}));
        expect(screen.getByText('Chyba serveru')).toBeInTheDocument();
    });

    it('renders "Vytvořit skupinu" button when createGroup template exists', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/groups'}},
            _templates: {
                createGroup: mockHalFormsTemplate({title: 'Vytvořit skupinu', method: 'POST'}),
            },
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /vytvořit skupinu/i})).toBeInTheDocument();
    });

    it('does not render create button when createGroup template is absent', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/groups'}},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /vytvořit skupinu/i})).not.toBeInTheDocument();
    });

    it('renders group names in the table', () => {
        renderPageWithGroups([buildGroupRow({name: 'Závoďáci'})]);
        expect(screen.getByText('Závoďáci')).toBeInTheDocument();
    });
});
