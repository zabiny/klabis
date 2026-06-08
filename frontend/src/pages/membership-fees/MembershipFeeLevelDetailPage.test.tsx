import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembershipFeeLevelDetailPage} from './MembershipFeeLevelDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
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

const buildFeeLevelDetail = (overrides?: Partial<HalResponse>): HalResponse => ({
    id: 'level-1',
    name: 'Základní členství',
    annualFee: 500,
    coParticipationRules: [
        {
            raceTypeId: 'sprint',
            ranking: 'A',
            ruleType: 'PERCENTAGE',
            value: 50,
        },
    ],
    _links: {self: {href: '/api/membership-fee-levels/level-1'}},
    ...overrides,
});

const createMockPageData = (resourceData: HalResponse | null, overrides?: Record<string, unknown>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/administration/membership-fee-levels/level-1',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/membership-fee-levels/level-1'}),
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
            <MemoryRouter initialEntries={['/administration/membership-fee-levels/level-1']}>
                <MembershipFeeLevelDetailPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MembershipFeeLevelDetailPage', () => {
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

    it('renders the fee level name as heading', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByRole('heading', {name: /základní členství/i})).toBeInTheDocument();
    });

    it('renders annual fee value', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByText(/500/)).toBeInTheDocument();
    });

    it('renders "Roční poplatek" label', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByText('Roční poplatek')).toBeInTheDocument();
    });

    it('renders back link to list', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByText(/zpět na seznam/i)).toBeInTheDocument();
    });

    it('renders co-participation rules table with ranking column', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByText('Žebříček')).toBeInTheDocument();
    });

    it('renders co-participation rule data', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByText('sprint')).toBeInTheDocument();
        expect(screen.getByText('A')).toBeInTheDocument();
    });

    it('renders edit button when updateMembershipFeeLevel template exists', () => {
        const resourceData = buildFeeLevelDetail({
            _templates: {
                updateMembershipFeeLevel: mockHalFormsTemplate({title: 'Upravit úroveň', method: 'PATCH'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /upravit/i})).toBeInTheDocument();
    });

    it('renders delete button when deleteMembershipFeeLevel template exists', () => {
        const resourceData = buildFeeLevelDetail({
            _templates: {
                deleteMembershipFeeLevel: mockHalFormsTemplate({title: 'Smazat úroveň', method: 'DELETE'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /smazat/i})).toBeInTheDocument();
    });
});
