import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {CampaignDetail} from './CampaignDetail';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {mockHalFormsTemplate} from '../../__mocks__/halData';

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

const mockGroups = [
    {
        id: 'group-1',
        name: 'Základní členství',
        memberCount: 10,
        status: 'EDITABLE' as const,
        _links: {self: {href: '/api/membership-fee-groups/group-1'}},
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

const createMockPageData = (resourceData: HalResponse | null) => ({
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
});

const renderComponent = (pageData: ReturnType<typeof createMockPageData>) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData as ReturnType<typeof useHalPageData>);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <CampaignDetail/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('CampaignDetail', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders campaign year', () => {
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(screen.getByText('2025')).toBeInTheDocument();
    });

    it('renders voting deadline label', () => {
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(screen.getByText('Uzávěrka hlasování')).toBeInTheDocument();
    });

    it('renders fee groups section heading', () => {
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(screen.getByRole('heading', {name: /skupiny/i})).toBeInTheDocument();
    });

    it('does not render change-deadline button when changeDeadline template is absent', () => {
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(screen.queryByRole('button', {name: /změnit deadline/i})).not.toBeInTheDocument();
    });

    it('renders change-deadline button when changeDeadline template is present', () => {
        renderComponent(createMockPageData(buildCampaignDetail({
            _templates: {
                changeDeadline: mockHalFormsTemplate({title: 'Změnit deadline', method: 'PATCH'}),
            },
        })));
        expect(screen.getByRole('button', {name: /změnit deadline/i})).toBeInTheDocument();
    });

    it('does not render close-campaign button when closeCampaign template is absent', () => {
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(screen.queryByRole('button', {name: /uzavřít kampaň/i})).not.toBeInTheDocument();
    });

    it('renders close-campaign button when closeCampaign template is present', () => {
        renderComponent(createMockPageData(buildCampaignDetail({
            _templates: {
                closeCampaign: mockHalFormsTemplate({title: 'Uzavřít kampaň', method: 'POST', properties: []}),
            },
        })));
        expect(screen.getByRole('button', {name: /uzavřít kampaň/i})).toBeInTheDocument();
    });

    it('renders both buttons when both templates are present', () => {
        renderComponent(createMockPageData(buildCampaignDetail({
            _templates: {
                changeDeadline: mockHalFormsTemplate({title: 'Změnit deadline', method: 'PATCH'}),
                closeCampaign: mockHalFormsTemplate({title: 'Uzavřít kampaň', method: 'POST', properties: []}),
            },
        })));
        expect(screen.getByRole('button', {name: /změnit deadline/i})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /uzavřít kampaň/i})).toBeInTheDocument();
    });

    it('renders fee groups when groups data is available', () => {
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: {_embedded: {membershipFeeGroupResponseList: mockGroups}},
            error: null,
        } as ReturnType<typeof useAuthorizedQuery>);
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(screen.getByText('Základní členství')).toBeInTheDocument();
    });
});
