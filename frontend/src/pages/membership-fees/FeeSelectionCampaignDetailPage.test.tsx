import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {FeeSelectionCampaignDetailPage} from './FeeSelectionCampaignDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {mockHalFormsTemplate} from '../../__mocks__/halData';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
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

const mockGroups = [
    {
        id: 'group-1',
        name: 'Základní členství',
        memberCount: 10,
        status: 'EDITABLE',
        _links: {self: {href: '/api/membership-fee-groups/group-1'}},
    },
    {
        id: 'group-2',
        name: 'Aktivní závodník',
        memberCount: 5,
        status: 'FROZEN',
        _links: {self: {href: '/api/membership-fee-groups/group-2'}},
    },
];

const buildCampaignDetail = (overrides?: Partial<HalResponse>): HalResponse => ({
    id: 'campaign-1',
    year: 2025,
    votingDeadline: '2025-03-31',
    _links: {
        self: {href: '/api/fee-selection-campaigns/campaign-1'},
        levels: {href: '/api/fee-selection-campaigns/campaign-1/levels'},
    },
    ...overrides,
});

const createMockPageData = (resourceData: HalResponse | null, overrides?: Record<string, unknown>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/administration/fee-selection-campaigns/campaign-1',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/fee-selection-campaigns/campaign-1'}),
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
            <MemoryRouter initialEntries={['/administration/fee-selection-campaigns/campaign-1']}>
                <FeeSelectionCampaignDetailPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('FeeSelectionCampaignDetailPage', () => {
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

    it('renders year as heading', () => {
        renderPage(createMockPageData(buildCampaignDetail()));
        expect(screen.getByRole('heading', {name: /2025/i})).toBeInTheDocument();
    });

    it('renders choice deadline label', () => {
        renderPage(createMockPageData(buildCampaignDetail()));
        expect(screen.getByText('Uzávěrka hlasování')).toBeInTheDocument();
    });

    it('renders back link to list', () => {
        renderPage(createMockPageData(buildCampaignDetail()));
        expect(screen.getByText(/zpět na seznam/i)).toBeInTheDocument();
    });

    it('renders groups section heading', () => {
        renderPage(createMockPageData(buildCampaignDetail()));
        expect(screen.getByRole('heading', {name: /skupiny/i})).toBeInTheDocument();
    });

    it('renders group names', () => {
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: {_embedded: {membershipFeeGroupResponseList: mockGroups}},
            error: null,
        } as ReturnType<typeof useAuthorizedQuery>);
        renderPage(createMockPageData(buildCampaignDetail()));
        expect(screen.getByText('Základní členství')).toBeInTheDocument();
        expect(screen.getByText('Aktivní závodník')).toBeInTheDocument();
    });

    it('renders EDITABLE group status as "Editovatelná"', () => {
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: {_embedded: {membershipFeeGroupResponseList: mockGroups}},
            error: null,
        } as ReturnType<typeof useAuthorizedQuery>);
        renderPage(createMockPageData(buildCampaignDetail()));
        expect(screen.getByText('Editovatelná')).toBeInTheDocument();
    });

    it('renders FROZEN group status as "Zmrazená"', () => {
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: {_embedded: {membershipFeeGroupResponseList: mockGroups}},
            error: null,
        } as ReturnType<typeof useAuthorizedQuery>);
        renderPage(createMockPageData(buildCampaignDetail()));
        expect(screen.getByText('Zmrazená')).toBeInTheDocument();
    });

    it('renders links to group details', () => {
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: {_embedded: {membershipFeeGroupResponseList: mockGroups}},
            error: null,
        } as ReturnType<typeof useAuthorizedQuery>);
        renderPage(createMockPageData(buildCampaignDetail()));
        const links = screen.getAllByRole('link');
        const groupLinks = links.filter(l => l.getAttribute('href')?.includes('membership-fee-groups'));
        expect(groupLinks.length).toBeGreaterThan(0);
    });

    it('does not show "Změnit deadline" button when changeDeadline template is absent', () => {
        renderPage(createMockPageData(buildCampaignDetail()));
        expect(screen.queryByRole('button', {name: /změnit deadline/i})).not.toBeInTheDocument();
    });

    it('shows "Změnit deadline" button when changeDeadline template is present', () => {
        const resourceData = buildCampaignDetail({
            _templates: {
                changeDeadline: mockHalFormsTemplate({title: 'Změnit deadline', method: 'PATCH'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /změnit deadline/i})).toBeInTheDocument();
    });

    it('does not show "Uzavřít kampaň" button when closeCampaign template is absent', () => {
        renderPage(createMockPageData(buildCampaignDetail()));
        expect(screen.queryByRole('button', {name: /uzavřít kampaň/i})).not.toBeInTheDocument();
    });

    it('shows "Uzavřít kampaň" button when closeCampaign template is present', () => {
        const resourceData = buildCampaignDetail({
            _templates: {
                closeCampaign: mockHalFormsTemplate({title: 'Uzavřít kampaň', method: 'POST', properties: []}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /uzavřít kampaň/i})).toBeInTheDocument();
    });
});
