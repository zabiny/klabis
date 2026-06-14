import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {PastCampaignsSection} from './PastCampaignsSection';
import {vi} from 'vitest';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

const mockCampaigns = [
    {
        id: 'campaign-2024',
        year: 2024,
        votingDeadline: '2024-03-31',
        _links: {self: {href: '/api/fee-selection-campaigns/campaign-2024'}},
    },
    {
        id: 'campaign-2023',
        year: 2023,
        votingDeadline: '2023-03-31',
        _links: {self: {href: '/api/fee-selection-campaigns/campaign-2023'}},
    },
];

const renderSection = (pastCampaignsHref: string | null) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <PastCampaignsSection pastCampaignsHref={pastCampaignsHref}/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('PastCampaignsSection', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders section heading "Minulé kampaně"', () => {
        renderSection('/api/fee-selection-campaigns?status=closed');
        expect(screen.getByRole('heading', {name: /minulé kampaně/i})).toBeInTheDocument();
    });

    describe('empty state', () => {
        it('renders empty state message when no past campaigns are returned', () => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {_embedded: {feeSelectionCampaignResponseList: []}},
                error: null,
            } as ReturnType<typeof useAuthorizedQuery>);
            renderSection('/api/fee-selection-campaigns?status=closed');
            expect(screen.getByText(/žádné minulé kampaně/i)).toBeInTheDocument();
        });
    });

    describe('campaign list', () => {
        beforeEach(() => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {
                    _embedded: {feeSelectionCampaignResponseList: mockCampaigns},
                    page: {size: 10, totalElements: 2, totalPages: 1, number: 0},
                },
                error: null,
            } as ReturnType<typeof useAuthorizedQuery>);
        });

        it('renders campaign years', () => {
            renderSection('/api/fee-selection-campaigns?status=closed');
            expect(screen.getByText('2024')).toBeInTheDocument();
            expect(screen.getByText('2023')).toBeInTheDocument();
        });

        it('renders navigation to campaign detail for each campaign row', () => {
            renderSection('/api/fee-selection-campaigns?status=closed');
            const links = screen.getAllByRole('link');
            const campaignLinks = links.filter(l => l.getAttribute('href')?.includes('fee-selection-campaigns'));
            expect(campaignLinks.length).toBeGreaterThan(0);
        });
    });

    describe('when pastCampaignsHref is null', () => {
        it('renders nothing when pastCampaignsHref is null', () => {
            const {container} = renderSection(null);
            expect(container).toBeEmptyDOMElement();
        });
    });
});
