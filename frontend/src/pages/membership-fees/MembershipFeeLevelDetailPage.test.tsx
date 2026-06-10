import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembershipFeeLevelDetailPage} from './MembershipFeeLevelDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import type {HalFormDisplayProps} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import type {HalFormModalProps} from '../../components/HalNavigator2/HalFormModal.tsx';

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

vi.mock('../../components/HalNavigator2/HalFormDisplay.tsx', () => ({
    HalFormDisplay: ({submitButtonLabel}: HalFormDisplayProps) => (
        <button type="submit">{submitButtonLabel ?? 'Odeslat'}</button>
    ),
}));

vi.mock('../../components/HalNavigator2/HalFormModal.tsx', () => ({
    HalFormModal: ({title, onClose}: HalFormModalProps) => (
        <div role="dialog" aria-label={title}>
            <button onClick={onClose}>Zavřít</button>
        </div>
    ),
}));

const buildFeeLevelDetail = (overrides?: Partial<HalResponse>): HalResponse => ({
    id: 'level-1',
    name: 'Základní členství',
    yearlyFeeAmount: 500,
    yearlyFeeCurrency: 'CZK',
    rules: [
        {
            eventTypeId: 'sprint',
            rankingShortName: 'A',
            ruleType: 'PERCENTAGE',
            percent: 50,
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

    it('renders the fee level name as page heading', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByRole('heading', {name: /základní informace/i})).toBeInTheDocument();
    });

    it('renders annual fee value', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByText(/500/)).toBeInTheDocument();
    });

    it('renders "Roční poplatek" label', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByText('Roční poplatek (Kč)')).toBeInTheDocument();
    });

    it('renders breadcrumb with link back to list and current name', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByRole('link', {name: 'Katalog úrovní'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Katalog úrovní'})).toHaveAttribute('href', '/membership-fee-levels');
    });

    it('renders co-participation rules section heading always', () => {
        renderPage(createMockPageData(buildFeeLevelDetail({rules: []})));
        expect(screen.getByText('Pravidla spoluúčasti')).toBeInTheDocument();
    });

    it('renders co-participation rules table headers', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByText('Typ závodu')).toBeInTheDocument();
        expect(screen.getByText('Žebříček')).toBeInTheDocument();
        expect(screen.getByText('Typ pravidla')).toBeInTheDocument();
    });

    it('renders PERCENTAGE rule with eventTypeId and rankingShortName', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.getByText('sprint')).toBeInTheDocument();
        expect(screen.getByText('A')).toBeInTheDocument();
        expect(screen.getByText('50 %')).toBeInTheDocument();
    });

    it('renders FIXED_AMOUNT rule with fixedAmount and currency', () => {
        const resourceData = buildFeeLevelDetail({
            rules: [
                {
                    eventTypeId: 'relay',
                    rankingShortName: 'B',
                    ruleType: 'FIXED_AMOUNT',
                    fixedAmount: 200,
                    fixedCurrency: 'CZK',
                },
            ],
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('relay')).toBeInTheDocument();
        expect(screen.getByText('B')).toBeInTheDocument();
        expect(screen.getByText('200 CZK')).toBeInTheDocument();
    });

    it('renders multiple rules', () => {
        const resourceData = buildFeeLevelDetail({
            rules: [
                {eventTypeId: 'sprint', rankingShortName: 'A', ruleType: 'PERCENTAGE', percent: 50},
                {eventTypeId: 'relay', rankingShortName: 'B', ruleType: 'FIXED_AMOUNT', fixedAmount: 100, fixedCurrency: 'CZK'},
            ],
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText('sprint')).toBeInTheDocument();
        expect(screen.getByText('relay')).toBeInTheDocument();
        expect(screen.getByText('50 %')).toBeInTheDocument();
        expect(screen.getByText('100 CZK')).toBeInTheDocument();
    });

    it('renders save button inside basic info card when editLevel template exists (after clicking edit)', () => {
        const resourceData = buildFeeLevelDetail({
            _templates: {
                editLevel: mockHalFormsTemplate({title: 'Upravit úroveň', method: 'PATCH'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /upravit/i}));
        expect(screen.getByRole('button', {name: /uložit změny/i})).toBeInTheDocument();
    });

    it('does not render save button when editLevel template is absent', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.queryByRole('button', {name: /uložit změny/i})).not.toBeInTheDocument();
    });

    it('renders delete level button when deleteLevel template exists', () => {
        const resourceData = buildFeeLevelDetail({
            rules: [],
            _templates: {
                deleteLevel: mockHalFormsTemplate({title: 'Smazat úroveň', method: 'DELETE'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /smazat úroveň/i})).toBeInTheDocument();
    });

    it('renders add rule footer button when addRule template exists', () => {
        const resourceData = buildFeeLevelDetail({
            _templates: {
                addRule: mockHalFormsTemplate({title: 'Přidat pravidlo', method: 'POST'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText(/přidat pravidlo/i)).toBeInTheDocument();
    });

    it('does not render add rule footer when addRule template is absent', () => {
        renderPage(createMockPageData(buildFeeLevelDetail()));
        expect(screen.queryByText(/přidat pravidlo/i)).not.toBeInTheDocument();
    });
});
