import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {MembershipFeesAdminPage} from './MembershipFeesAdminPage';
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
        pathname: '/membership-fees',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/membership-fee-tiers'}),
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
            <MemoryRouter initialEntries={['/membership-fees']}>
                <MembershipFeesAdminPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const tierCatalogResource: HalResponse = {
    _links: {
        self: {href: '/api/membership-fee-tiers'},
    },
};

const tierCatalogResourceWithActiveCampaign: HalResponse = {
    _links: {
        self: {href: '/api/membership-fee-tiers'},
        activeCampaign: {href: '/api/fee-selection-campaigns/campaign-1'},
        pastCampaigns: {href: '/api/fee-selection-campaigns?status=closed'},
    },
};

const tierCatalogResourceWithoutActiveCampaign: HalResponse = {
    _links: {
        self: {href: '/api/membership-fee-tiers'},
        pastCampaigns: {href: '/api/fee-selection-campaigns?status=closed'},
    },
};

describe('MembershipFeesAdminPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows skeleton loading state when data is loading', () => {
        renderPage(createMockPageData(null, {isLoading: true}));
        expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
    });

    it('shows error message when fetch fails', () => {
        renderPage(createMockPageData(null, {error: new Error('Chyba serveru')}));
        expect(screen.getByText('Chyba serveru')).toBeInTheDocument();
    });

    it('renders page heading "Členské příspěvky"', () => {
        renderPage(createMockPageData(tierCatalogResource));
        expect(screen.getByRole('heading', {level: 1, name: /členské příspěvky/i})).toBeInTheDocument();
    });

    it('renders TierCatalogSection heading "Katalog tierů"', () => {
        renderPage(createMockPageData(tierCatalogResource));
        expect(screen.getByRole('heading', {name: /katalog tierů/i})).toBeInTheDocument();
    });

    describe('when activeCampaign link is present', () => {
        it('renders ActiveCampaignSection heading', () => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {
                    id: 'campaign-1',
                    year: 2025,
                    votingDeadline: '2025-03-31',
                    _links: {
                        self: {href: '/api/fee-selection-campaigns/campaign-1'},
                        levels: {href: '/api/fee-selection-campaigns/campaign-1/levels'},
                    },
                },
                error: null,
            } as ReturnType<typeof useAuthorizedQuery>);
            renderPage(createMockPageData(tierCatalogResourceWithActiveCampaign));
            expect(screen.getByRole('heading', {name: /aktivní kampaň/i})).toBeInTheDocument();
        });
    });

    describe('when activeCampaign link is absent', () => {
        it('does not render ActiveCampaignSection heading', () => {
            renderPage(createMockPageData(tierCatalogResourceWithoutActiveCampaign));
            expect(screen.queryByRole('heading', {name: /aktivní kampaň/i})).not.toBeInTheDocument();
        });

        it('still renders TierCatalogSection', () => {
            renderPage(createMockPageData(tierCatalogResourceWithoutActiveCampaign));
            expect(screen.getByRole('heading', {name: /katalog tierů/i})).toBeInTheDocument();
        });

        it('renders PastCampaignsSection heading', () => {
            renderPage(createMockPageData(tierCatalogResourceWithoutActiveCampaign));
            expect(screen.getByRole('heading', {name: /minulé kampaně/i})).toBeInTheDocument();
        });
    });

    describe('when both activeCampaign and pastCampaigns links are present', () => {
        it('renders both ActiveCampaignSection and PastCampaignsSection', () => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {
                    id: 'campaign-1',
                    year: 2025,
                    votingDeadline: '2025-03-31',
                    _links: {
                        self: {href: '/api/fee-selection-campaigns/campaign-1'},
                        levels: {href: '/api/fee-selection-campaigns/campaign-1/levels'},
                    },
                },
                error: null,
            } as ReturnType<typeof useAuthorizedQuery>);
            renderPage(createMockPageData(tierCatalogResourceWithActiveCampaign));
            expect(screen.getByRole('heading', {name: /aktivní kampaň/i})).toBeInTheDocument();
            expect(screen.getByRole('heading', {name: /minulé kampaně/i})).toBeInTheDocument();
        });
    });

    it('renders all three section headings when all links are present', () => {
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: {
                id: 'campaign-1',
                year: 2025,
                votingDeadline: '2025-03-31',
                _links: {
                    self: {href: '/api/fee-selection-campaigns/campaign-1'},
                    levels: {href: '/api/fee-selection-campaigns/campaign-1/levels'},
                },
            },
            error: null,
        } as ReturnType<typeof useAuthorizedQuery>);
        renderPage(createMockPageData(tierCatalogResourceWithActiveCampaign));
        expect(screen.getByRole('heading', {name: /aktivní kampaň/i})).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: /katalog tierů/i})).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: /minulé kampaně/i})).toBeInTheDocument();
    });
});
