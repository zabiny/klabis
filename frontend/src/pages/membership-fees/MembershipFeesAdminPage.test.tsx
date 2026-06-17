import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
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

vi.mock('../../components/HalNavigator2/HalFormModal', () => ({
    HalFormModal: ({title}: {title: string}) => <div data-testid="hal-form-modal">{title}</div>,
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalSubresourceProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../components/membership-fees/CampaignDetail', () => ({
    CampaignDetail: () => <div data-testid="campaign-detail">Campaign Detail</div>,
}));

const {mockDisplayHalForm} = vi.hoisted(() => ({mockDisplayHalForm: vi.fn()}));

vi.mock('../../contexts/halFormContext.ts', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: mockDisplayHalForm,
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
        pathname: '/membership-fee-tiers',
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
            <MemoryRouter initialEntries={['/membership-fee-tiers']}>
                <MembershipFeesAdminPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const publishYearTemplate = {
    method: 'POST' as const,
    target: '/api/fee-selection-campaigns',
    properties: [{name: 'year', type: 'number', required: true}],
};

const tierCatalogResource: HalResponse = {
    _links: {
        self: {href: '/api/membership-fee-tiers'},
    },
};

const tierCatalogResourceWithPublishYear: HalResponse = {
    _links: {self: {href: '/api/membership-fee-tiers'}},
    _templates: {publishYear: publishYearTemplate},
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
        mockDisplayHalForm.mockClear();
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
        it('renders active campaign section heading', () => {
            renderPage(createMockPageData(tierCatalogResourceWithActiveCampaign));
            expect(screen.getByRole('heading', {name: /aktivní kampaň/i})).toBeInTheDocument();
        });

        it('renders CampaignDetail component', () => {
            renderPage(createMockPageData(tierCatalogResourceWithActiveCampaign));
            expect(screen.getByTestId('campaign-detail')).toBeInTheDocument();
        });
    });

    describe('when activeCampaign link is absent', () => {
        it('does not render active campaign section heading', () => {
            renderPage(createMockPageData(tierCatalogResourceWithoutActiveCampaign));
            expect(screen.queryByRole('heading', {name: /aktivní kampaň/i})).not.toBeInTheDocument();
        });

        it('does not render CampaignDetail component', () => {
            renderPage(createMockPageData(tierCatalogResourceWithoutActiveCampaign));
            expect(screen.queryByTestId('campaign-detail')).not.toBeInTheDocument();
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
        it('renders both active campaign section and PastCampaignsSection', () => {
            renderPage(createMockPageData(tierCatalogResourceWithActiveCampaign));
            expect(screen.getByRole('heading', {name: /aktivní kampaň/i})).toBeInTheDocument();
            expect(screen.getByRole('heading', {name: /minulé kampaně/i})).toBeInTheDocument();
        });
    });

    it('renders all three section headings when all links are present', () => {
        renderPage(createMockPageData(tierCatalogResourceWithActiveCampaign));
        expect(screen.getByRole('heading', {name: /aktivní kampaň/i})).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: /katalog tierů/i})).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: /minulé kampaně/i})).toBeInTheDocument();
    });

    describe('when publishYear template is present and no activeCampaign', () => {
        it('renders publishYear button', () => {
            renderPage(createMockPageData(tierCatalogResourceWithPublishYear));
            expect(screen.getByRole('button', {name: /vypsat kampaň/i})).toBeInTheDocument();
        });

        it('opens modal when publishYear button is clicked', async () => {
            const user = userEvent.setup();
            renderPage(createMockPageData(tierCatalogResourceWithPublishYear));
            await user.click(screen.getByRole('button', {name: /vypsat kampaň/i}));
            expect(mockDisplayHalForm).toHaveBeenCalledWith(
                expect.objectContaining({templateName: 'publishYear', modal: true}),
            );
        });
    });

    describe('when publishYear template is absent', () => {
        it('does not render publishYear button', () => {
            renderPage(createMockPageData(tierCatalogResource));
            expect(screen.queryByRole('button', {name: /vypsat kampaň/i})).not.toBeInTheDocument();
        });
    });

    describe('when activeCampaign link is present (even if publishYear template exists)', () => {
        it('does not render publishYear button when activeCampaign link exists', () => {
            const resourceWithBoth: HalResponse = {
                _links: {
                    self: {href: '/api/membership-fee-tiers'},
                    activeCampaign: {href: '/api/fee-selection-campaigns/campaign-1'},
                },
                _templates: {publishYear: publishYearTemplate},
            };
            renderPage(createMockPageData(resourceWithBoth));
            expect(screen.queryByRole('button', {name: /vypsat kampaň/i})).not.toBeInTheDocument();
        });
    });
});
