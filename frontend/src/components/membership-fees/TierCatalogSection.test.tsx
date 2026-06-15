import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {TierCatalogSection} from './TierCatalogSection';
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
        pathname: '/administration/membership-fees',
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

const renderSection = (pageData: ReturnType<typeof createMockPageData>) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData as ReturnType<typeof useHalPageData>);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/administration/membership-fees']}>
                <TierCatalogSection/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('TierCatalogSection', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders section heading "Katalog tierů"', () => {
        renderSection(createMockPageData(null));
        expect(screen.getByRole('heading', {name: /katalog tierů/i})).toBeInTheDocument();
    });

    it('renders table column headers for name and yearly fee amount', () => {
        renderSection(createMockPageData(null));
        expect(screen.getByText('Název')).toBeInTheDocument();
        expect(screen.getByText('Roční poplatek')).toBeInTheDocument();
    });

    it('renders "Přidat tier" create button when createTier template is present', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/membership-fee-tiers'}},
            _templates: {
                createTier: mockHalFormsTemplate({title: 'Přidat tier', method: 'POST'}),
            },
        };
        renderSection(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /přidat tier/i})).toBeInTheDocument();
    });

    it('does not render create button when createTier template is absent', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/membership-fee-tiers'}},
        };
        renderSection(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /přidat tier/i})).not.toBeInTheDocument();
    });

    it('renders tier name when embedded tiers are available', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/membership-fee-tiers'}},
            _embedded: {
                membershipFeeTierSummaryResponseList: [{
                    id: 'tier-1',
                    name: 'Základní členství',
                    yearlyFeeAmount: 500,
                    _links: {self: {href: '/api/membership-fee-tiers/tier-1'}},
                }],
            },
            page: {size: 10, totalElements: 1, totalPages: 1, number: 0},
        };
        vi.mocked(useAuthorizedQuery).mockReturnValue({data: resourceData, error: null} as ReturnType<typeof useAuthorizedQuery>);
        renderSection(createMockPageData(resourceData));
        expect(screen.getByText('Základní členství')).toBeInTheDocument();
    });

    it('navigates to tier detail when a table row is clicked', () => {
        const navigateToResource = vi.fn();
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/membership-fee-tiers'}},
            _embedded: {
                membershipFeeTierSummaryResponseList: [{
                    id: 'tier-1',
                    name: 'Základní členství',
                    yearlyFeeAmount: 500,
                    _links: {self: {href: '/api/membership-fee-tiers/tier-1'}},
                }],
            },
            page: {size: 10, totalElements: 1, totalPages: 1, number: 0},
        };
        vi.mocked(useAuthorizedQuery).mockReturnValue({data: resourceData, error: null} as ReturnType<typeof useAuthorizedQuery>);
        renderSection(createMockPageData(resourceData, {route: {
            pathname: '/administration/membership-fees',
            navigateToResource,
            refetch: async () => {},
            queryState: 'success' as const,
            getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/membership-fee-tiers'}),
        }}));
        screen.getByText('Základní členství').closest('tr')!.click();
        expect(navigateToResource).toHaveBeenCalled();
    });
});
