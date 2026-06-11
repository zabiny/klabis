import '@testing-library/jest-dom';
import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembershipFeeTierDetailPage} from './MembershipFeeTierDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import type {HalFormDisplayProps} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import type {HalFormModalProps} from '../../components/HalNavigator2/HalFormModal.tsx';
import {authorizedFetch} from '../../api/authorizedFetch';
import {useEventTypes} from '../../hooks/useEventTypes';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../api/authorizedFetch', () => ({
    authorizedFetch: vi.fn(),
}));

vi.mock('../../hooks/useEventTypes', () => ({
    useEventTypes: vi.fn().mockReturnValue({
        eventTypes: [],
        isLoading: false,
        getById: vi.fn().mockReturnValue(undefined),
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

const buildFeeTierDetail = (overrides?: Partial<HalResponse>): HalResponse => ({
    id: 'tier-1',
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
    _links: {self: {href: '/api/membership-fee-tiers/tier-1'}},
    ...overrides,
});

const createMockPageData = (resourceData: HalResponse | null, overrides?: Record<string, unknown>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/administration/membership-fee-tiers/tier-1',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/membership-fee-tiers/tier-1'}),
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
            <MemoryRouter initialEntries={['/administration/membership-fee-tiers/tier-1']}>
                <MembershipFeeTierDetailPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MembershipFeeTierDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(useEventTypes).mockReturnValue({
            eventTypes: [],
            isLoading: false,
            getById: vi.fn().mockReturnValue(undefined),
        });
    });

    it('shows skeleton loading state when data is loading', () => {
        renderPage(createMockPageData(null, {isLoading: true}));
        expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
    });

    it('shows error message when fetch fails', () => {
        renderPage(createMockPageData(null, {error: new Error('Chyba serveru')}));
        expect(screen.getByText('Chyba serveru')).toBeInTheDocument();
    });

    it('renders the fee tier name as page heading', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByRole('heading', {name: /základní informace/i})).toBeInTheDocument();
    });

    it('renders annual fee value', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByText(/500/)).toBeInTheDocument();
    });

    it('renders "Roční poplatek" label', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByText('Roční poplatek (Kč)')).toBeInTheDocument();
    });

    it('renders breadcrumb with link back to list and current name', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByRole('link', {name: 'Katalog tierů'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Katalog tierů'})).toHaveAttribute('href', '/membership-fee-tiers');
    });

    it('renders co-participation rules section heading always', () => {
        renderPage(createMockPageData(buildFeeTierDetail({rules: []})));
        expect(screen.getByText('Pravidla spoluúčasti')).toBeInTheDocument();
    });

    it('renders co-participation rules table headers', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByText('Typ závodu')).toBeInTheDocument();
        expect(screen.getByText('Žebříček')).toBeInTheDocument();
        expect(screen.getByText('Typ pravidla')).toBeInTheDocument();
    });

    it('renders PERCENTAGE rule with eventTypeId and rankingShortName', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByText('sprint')).toBeInTheDocument();
        expect(screen.getByText('A')).toBeInTheDocument();
        expect(screen.getByText('50 %')).toBeInTheDocument();
    });

    it('shows event type name instead of raw id when useEventTypes resolves a match', () => {
        vi.mocked(useEventTypes).mockReturnValue({
            eventTypes: [{id: 'sprint', name: 'Sprint závod', sortOrder: 1}],
            isLoading: false,
            getById: (id) => id === 'sprint' ? {id: 'sprint', name: 'Sprint závod', sortOrder: 1} : undefined,
        });
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByText('Sprint závod')).toBeInTheDocument();
        expect(screen.queryByText('sprint')).not.toBeInTheDocument();
    });

    it('renders FIXED_AMOUNT rule with fixedAmount and currency', () => {
        const resourceData = buildFeeTierDetail({
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
        const resourceData = buildFeeTierDetail({
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

    it('renders save button inside basic info card when editTier template exists (after clicking edit)', () => {
        const resourceData = buildFeeTierDetail({
            _templates: {
                editTier: mockHalFormsTemplate({title: 'Upravit tier', method: 'PATCH'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /upravit/i}));
        expect(screen.getByRole('button', {name: /uložit změny/i})).toBeInTheDocument();
    });

    it('does not render save button when editTier template is absent', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.queryByRole('button', {name: /uložit změny/i})).not.toBeInTheDocument();
    });

    it('renders delete tier button when deleteTier template exists', () => {
        const resourceData = buildFeeTierDetail({
            rules: [],
            _templates: {
                deleteTier: mockHalFormsTemplate({title: 'Smazat tier', method: 'DELETE'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /smazat tier/i})).toBeInTheDocument();
    });

    it('renders add rule footer button when addRule template exists', () => {
        const resourceData = buildFeeTierDetail({
            _templates: {
                addRule: mockHalFormsTemplate({title: 'Přidat pravidlo', method: 'POST'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        expect(screen.getByText(/přidat pravidlo/i)).toBeInTheDocument();
    });

    it('does not render add rule footer when addRule template is absent', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.queryByText(/přidat pravidlo/i)).not.toBeInTheDocument();
    });

    describe('per-rule edit button', () => {
        const buildTierWithEditRuleLink = (overrides?: Partial<HalResponse>): HalResponse =>
            buildFeeTierDetail({
                _links: {
                    self: {href: '/api/membership-fee-tiers/tier-1'},
                    editRule: {href: '/api/membership-fee-tiers/tier-1/rules/{eventTypeId}/{ranking}', templated: true},
                },
                ...overrides,
            });

        const mockEditRuleFetchResponse = () => {
            vi.mocked(authorizedFetch).mockResolvedValue({
                json: async () => ({
                    _links: {self: {href: '/api/membership-fee-tiers/tier-1/rules/sprint/A'}},
                    _templates: {
                        editRule: mockHalFormsTemplate({title: 'Upravit pravidlo', method: 'PATCH'}),
                    },
                }),
            } as unknown as Response);
        };

        it('renders edit button for each rule row when editRule link is present', () => {
            renderPage(createMockPageData(buildTierWithEditRuleLink()));
            expect(screen.getByRole('button', {name: /upravit pravidlo/i})).toBeInTheDocument();
        });

        it('does not render edit rule button when editRule link is absent', () => {
            renderPage(createMockPageData(buildFeeTierDetail()));
            expect(screen.queryByRole('button', {name: /upravit pravidlo/i})).not.toBeInTheDocument();
        });

        it('does not render edit rule button when only _templates.editRule exists without link (old tier response)', () => {
            const resourceData = buildFeeTierDetail({
                _templates: {
                    editRule: mockHalFormsTemplate({title: 'Upravit pravidlo', method: 'PATCH'}),
                },
            });
            renderPage(createMockPageData(resourceData));
            expect(screen.queryByRole('button', {name: /upravit pravidlo/i})).not.toBeInTheDocument();
        });

        it('opens edit rule modal after fetching rule data when edit button is clicked', async () => {
            mockEditRuleFetchResponse();
            renderPage(createMockPageData(buildTierWithEditRuleLink()));
            fireEvent.click(screen.getByRole('button', {name: /upravit pravidlo/i}));
            await waitFor(() => {
                expect(screen.getByRole('dialog', {name: /upravit pravidlo/i})).toBeInTheDocument();
            });
        });

        it('closes edit rule modal when close button is clicked', async () => {
            mockEditRuleFetchResponse();
            renderPage(createMockPageData(buildTierWithEditRuleLink()));
            fireEvent.click(screen.getByRole('button', {name: /upravit pravidlo/i}));
            await waitFor(() => expect(screen.getByRole('dialog', {name: /upravit pravidlo/i})).toBeInTheDocument());
            fireEvent.click(screen.getByRole('button', {name: /zavřít/i}));
            expect(screen.queryByRole('dialog', {name: /upravit pravidlo/i})).not.toBeInTheDocument();
        });

        it('renders edit buttons for each rule when multiple rules exist', () => {
            const resourceData = buildTierWithEditRuleLink({
                rules: [
                    {eventTypeId: 'sprint', rankingShortName: 'A', ruleType: 'PERCENTAGE', percent: 50},
                    {eventTypeId: 'relay', rankingShortName: 'B', ruleType: 'FIXED_AMOUNT', fixedAmount: 100, fixedCurrency: 'CZK'},
                ],
            });
            renderPage(createMockPageData(resourceData));
            expect(screen.getAllByRole('button', {name: /upravit pravidlo/i})).toHaveLength(2);
        });
    });
});
