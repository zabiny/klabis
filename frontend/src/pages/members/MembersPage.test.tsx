import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembersPage} from './MembersPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

const createMockPageData = (resourceData: HalResponse | null, overrides?: any) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/members',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/members'}),
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
            <MemoryRouter initialEntries={['/members']}>
                <MembersPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MembersPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders page title "Členové"', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByText('Členové')).toBeInTheDocument();
    });

    it('renders "Registrovat člena" link navigating to /members/new when template exists', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
            _templates: {
                default: mockHalFormsTemplate({title: 'Create Member'}),
            },
        };
        renderPage(createMockPageData(resourceData));
        const link = screen.getByRole('link', {name: /registrovat člena/i});
        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute('href', '/members/new');
    });

    it('does NOT render "Registrovat člena" link when template does not exist', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('link', {name: /registrovat člena/i})).not.toBeInTheDocument();
    });

    it('renders table columns', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('Reg. číslo')).toBeInTheDocument();
        expect(screen.getByText('Příjmení')).toBeInTheDocument();
        expect(screen.getByText('Jméno')).toBeInTheDocument();
        expect(screen.queryByText('E-mail')).not.toBeInTheDocument();
        expect(screen.queryByText('Stav')).not.toBeInTheDocument();
    });

    it('shows loading state when data is loading', () => {
        renderPage(createMockPageData(null, {isLoading: true}));
        expect(screen.getByText(/načítání/i)).toBeInTheDocument();
    });
});
