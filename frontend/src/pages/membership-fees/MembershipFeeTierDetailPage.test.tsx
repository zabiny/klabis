import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import type {UseHalPageDataReturn} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembershipFeeTierDetailPage} from './MembershipFeeTierDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import type {HalFormDisplayProps} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import type {HalFormModalProps} from '../../components/HalNavigator2/HalFormModal.tsx';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
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

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalSubresourceProvider: ({subresourceLinkName, children}: {subresourceLinkName: string; children: React.ReactNode}) => (
        <div data-testid={`subresource-${subresourceLinkName}`}>{children}</div>
    ),
}));

interface PaymentRuleItem {
    eventTypeId: string;
    rankingShortName: string;
    ruleType: 'PERCENTAGE' | 'FIXED_AMOUNT';
    percentage?: number;
    fixedAmount?: number;
    fixedCurrency?: string;
    _links?: Record<string, {href: string}>;
    _templates?: Record<string, unknown>;
}

const buildRulesCollection = (rules: PaymentRuleItem[]): HalResponse => ({
    _embedded: {
        paymentRuleResponseList: rules.map(rule => ({
            ...rule,
            _links: rule._links ?? {self: {href: `/api/membership-fee-tiers/tier-1/rules/${rule.eventTypeId}/${rule.rankingShortName}`}},
            _templates: rule._templates ?? {},
        })),
    },
    _links: {self: {href: '/api/membership-fee-tiers/tier-1/rules'}},
});

const buildFeeTierDetail = (overrides?: Partial<HalResponse>): HalResponse => ({
    id: 'tier-1',
    name: 'Základní členství',
    yearlyFeeAmount: 500,
    yearlyFeeCurrency: 'CZK',
    _links: {
        self: {href: '/api/membership-fee-tiers/tier-1'},
        rules: {href: '/api/membership-fee-tiers/tier-1/rules'},
    },
    ...overrides,
});

const createMockPageData = (resourceData: HalResponse | null, overrides?: Partial<UseHalPageDataReturn>): UseHalPageDataReturn => ({
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

const createMockRulesPageData = (rulesCollection: HalResponse | null, overrides?: Partial<UseHalPageDataReturn>): UseHalPageDataReturn => ({
    resourceData: rulesCollection,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/administration/membership-fee-tiers/tier-1/rules',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/membership-fee-tiers/tier-1/rules'}),
    },
    actions: {handleNavigateToItem: vi.fn()},
    getLinks: vi.fn(() => undefined),
    getTemplates: vi.fn(() => undefined),
    hasEmbedded: vi.fn(() => true),
    getEmbeddedItems: vi.fn(() => (rulesCollection?._embedded?.paymentRuleResponseList ?? []) as unknown[]),
    isCollection: vi.fn(() => true),
    hasLink: vi.fn(() => false),
    hasTemplate: vi.fn(() => false),
    hasForms: vi.fn(() => false),
    getPageMetadata: vi.fn(() => undefined),
    ...overrides,
});

/**
 * Sets up mocks for page (tier detail) and rules subresource.
 * Call order: MembershipFeeTierDetailPage → FeeTierDetailContent → RulesTableBody (+ repeated for rows)
 * Tier data needed for calls 1 and 2, rules collection for call 3+
 */
const setupPageDataMocks = (
    tierData: UseHalPageDataReturn,
    rulesData: UseHalPageDataReturn,
) => {
    vi.mocked(useHalPageData)
        .mockReset()
        .mockReturnValueOnce(tierData)
        .mockReturnValueOnce(tierData)
        .mockReturnValue(rulesData);
};

const renderPage = (tierData: UseHalPageDataReturn, rulesData?: UseHalPageDataReturn) => {
    const rulesPageData = rulesData ?? createMockRulesPageData(buildRulesCollection([]));
    setupPageDataMocks(tierData, rulesPageData);
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
        vi.mocked(useHalPageData).mockReset();
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

    it('renders breadcrumb with link back to merged fees page', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByRole('link', {name: 'Členské příspěvky'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Členské příspěvky'})).toHaveAttribute('href', '/membership-fees');
    });

    it('renders co-participation rules section heading always', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByText('Pravidla spoluúčasti')).toBeInTheDocument();
    });

    it('renders co-participation rules table headers', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByText('Typ závodu')).toBeInTheDocument();
        expect(screen.getByText('Žebříček')).toBeInTheDocument();
        expect(screen.getByText('Typ pravidla')).toBeInTheDocument();
    });

    it('renders rules subresource provider with correct link name', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByTestId('subresource-rules')).toBeInTheDocument();
    });

    it('renders PERCENTAGE rule with eventTypeId and rankingShortName', () => {
        const rules: PaymentRuleItem[] = [{
            eventTypeId: 'sprint',
            rankingShortName: 'A',
            ruleType: 'PERCENTAGE',
            percentage: 50,
        }];
        renderPage(
            createMockPageData(buildFeeTierDetail()),
            createMockRulesPageData(buildRulesCollection(rules)),
        );
        expect(screen.getByText('sprint')).toBeInTheDocument();
        expect(screen.getByText('A')).toBeInTheDocument();
        expect(screen.getByText('50 %')).toBeInTheDocument();
    });

    it('translates rankingShortName code to label using addRule template options', () => {
        const tierData = buildFeeTierDetail({
            _templates: {
                addRule: mockHalFormsTemplate({
                    title: 'Přidat pravidlo',
                    method: 'POST',
                    properties: [
                        {
                            name: 'rankingShortName',
                            prompt: 'Žebříček',
                            type: 'text',
                            options: {
                                inline: [
                                    {value: '2', prompt: 'Žebříček A'},
                                    {value: '3', prompt: 'Žebříček B'},
                                ],
                            },
                        },
                    ],
                }),
            },
        });
        const rules: PaymentRuleItem[] = [{
            eventTypeId: 'sprint',
            rankingShortName: '2',
            ruleType: 'PERCENTAGE',
            percentage: 50,
        }];
        renderPage(
            createMockPageData(tierData),
            createMockRulesPageData(buildRulesCollection(rules)),
        );
        expect(screen.getByText('Žebříček A')).toBeInTheDocument();
        expect(screen.queryByText('2')).not.toBeInTheDocument();
    });

    it('falls back to raw rankingShortName code when no matching option exists', () => {
        const tierData = buildFeeTierDetail({
            _templates: {
                addRule: mockHalFormsTemplate({
                    title: 'Přidat pravidlo',
                    method: 'POST',
                    properties: [
                        {
                            name: 'rankingShortName',
                            prompt: 'Žebříček',
                            type: 'text',
                            options: {
                                inline: [
                                    {value: '2', prompt: 'Žebříček A'},
                                ],
                            },
                        },
                    ],
                }),
            },
        });
        const rules: PaymentRuleItem[] = [{
            eventTypeId: 'sprint',
            rankingShortName: 'unknown',
            ruleType: 'PERCENTAGE',
            percentage: 50,
        }];
        renderPage(
            createMockPageData(tierData),
            createMockRulesPageData(buildRulesCollection(rules)),
        );
        expect(screen.getByText('unknown')).toBeInTheDocument();
    });

    it('renders FIXED_AMOUNT rule with fixedAmount and currency', () => {
        const rules: PaymentRuleItem[] = [{
            eventTypeId: 'relay',
            rankingShortName: 'B',
            ruleType: 'FIXED_AMOUNT',
            fixedAmount: 200,
            fixedCurrency: 'CZK',
        }];
        renderPage(
            createMockPageData(buildFeeTierDetail()),
            createMockRulesPageData(buildRulesCollection(rules)),
        );
        expect(screen.getByText('relay')).toBeInTheDocument();
        expect(screen.getByText('B')).toBeInTheDocument();
        expect(screen.getByText('200 CZK')).toBeInTheDocument();
    });

    it('renders multiple rules', () => {
        const rules: PaymentRuleItem[] = [
            {eventTypeId: 'sprint', rankingShortName: 'A', ruleType: 'PERCENTAGE', percentage: 50},
            {eventTypeId: 'relay', rankingShortName: 'B', ruleType: 'FIXED_AMOUNT', fixedAmount: 100, fixedCurrency: 'CZK'},
        ];
        renderPage(
            createMockPageData(buildFeeTierDetail()),
            createMockRulesPageData(buildRulesCollection(rules)),
        );
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
        it('renders edit button for each rule row when editRule template is present in rule item', () => {
            const rules: PaymentRuleItem[] = [{
                eventTypeId: 'sprint',
                rankingShortName: 'A',
                ruleType: 'PERCENTAGE',
                percentage: 50,
                _templates: {
                    editRule: mockHalFormsTemplate({title: 'Upravit pravidlo', method: 'PATCH'}),
                },
            }];
            renderPage(
                createMockPageData(buildFeeTierDetail()),
                createMockRulesPageData(buildRulesCollection(rules)),
            );
            expect(screen.getByRole('button', {name: /upravit pravidlo/i})).toBeInTheDocument();
        });

        it('does not render edit button when rule item has no editRule template', () => {
            const rules: PaymentRuleItem[] = [{
                eventTypeId: 'sprint',
                rankingShortName: 'A',
                ruleType: 'PERCENTAGE',
                percentage: 50,
            }];
            renderPage(
                createMockPageData(buildFeeTierDetail()),
                createMockRulesPageData(buildRulesCollection(rules)),
            );
            expect(screen.queryByRole('button', {name: /upravit pravidlo/i})).not.toBeInTheDocument();
        });

        it('renders edit buttons for multiple rules', () => {
            const rules: PaymentRuleItem[] = [
                {
                    eventTypeId: 'sprint',
                    rankingShortName: 'A',
                    ruleType: 'PERCENTAGE',
                    percentage: 50,
                    _templates: {
                        editRule: mockHalFormsTemplate({title: 'Upravit pravidlo', method: 'PATCH'}),
                    },
                },
                {
                    eventTypeId: 'relay',
                    rankingShortName: 'B',
                    ruleType: 'FIXED_AMOUNT',
                    fixedAmount: 100,
                    fixedCurrency: 'CZK',
                    _templates: {
                        editRule: mockHalFormsTemplate({title: 'Upravit pravidlo', method: 'PATCH'}),
                    },
                },
            ];
            renderPage(
                createMockPageData(buildFeeTierDetail()),
                createMockRulesPageData(buildRulesCollection(rules)),
            );
            expect(screen.getAllByRole('button', {name: /upravit pravidlo/i})).toHaveLength(2);
        });

        it('opens editRule modal when edit button is clicked', () => {
            const rules: PaymentRuleItem[] = [{
                eventTypeId: 'sprint',
                rankingShortName: 'A',
                ruleType: 'PERCENTAGE',
                percentage: 50,
                _templates: {
                    editRule: mockHalFormsTemplate({title: 'Upravit pravidlo', method: 'PATCH'}),
                },
            }];
            renderPage(
                createMockPageData(buildFeeTierDetail()),
                createMockRulesPageData(buildRulesCollection(rules)),
            );
            fireEvent.click(screen.getByRole('button', {name: /upravit pravidlo/i}));
            expect(screen.getByRole('dialog', {name: 'Upravit pravidlo'})).toBeInTheDocument();
        });

        it('closes editRule modal when close button is clicked', () => {
            const rules: PaymentRuleItem[] = [{
                eventTypeId: 'sprint',
                rankingShortName: 'A',
                ruleType: 'PERCENTAGE',
                percentage: 50,
                _templates: {
                    editRule: mockHalFormsTemplate({title: 'Upravit pravidlo', method: 'PATCH'}),
                },
            }];
            renderPage(
                createMockPageData(buildFeeTierDetail()),
                createMockRulesPageData(buildRulesCollection(rules)),
            );
            fireEvent.click(screen.getByRole('button', {name: /upravit pravidlo/i}));
            fireEvent.click(screen.getByRole('button', {name: /zavřít/i}));
            expect(screen.queryByRole('dialog', {name: 'Upravit pravidlo'})).not.toBeInTheDocument();
        });
    });

    describe('per-rule delete button', () => {
        it('renders delete button for each rule row when removeRule template is present', () => {
            const rules: PaymentRuleItem[] = [{
                eventTypeId: 'sprint',
                rankingShortName: 'A',
                ruleType: 'PERCENTAGE',
                percentage: 50,
                _templates: {
                    removeRule: mockHalFormsTemplate({title: 'Smazat pravidlo', method: 'DELETE'}),
                },
            }];
            renderPage(
                createMockPageData(buildFeeTierDetail()),
                createMockRulesPageData(buildRulesCollection(rules)),
            );
            expect(screen.getByRole('button', {name: /smazat pravidlo/i})).toBeInTheDocument();
        });

        it('does not render delete button when rule item has no removeRule template', () => {
            const rules: PaymentRuleItem[] = [{
                eventTypeId: 'sprint',
                rankingShortName: 'A',
                ruleType: 'PERCENTAGE',
                percentage: 50,
            }];
            renderPage(
                createMockPageData(buildFeeTierDetail()),
                createMockRulesPageData(buildRulesCollection(rules)),
            );
            expect(screen.queryByRole('button', {name: /smazat pravidlo/i})).not.toBeInTheDocument();
        });

        it('opens removeRule modal when delete button is clicked', () => {
            const rules: PaymentRuleItem[] = [{
                eventTypeId: 'sprint',
                rankingShortName: 'A',
                ruleType: 'PERCENTAGE',
                percentage: 50,
                _templates: {
                    removeRule: mockHalFormsTemplate({title: 'Smazat pravidlo', method: 'DELETE'}),
                },
            }];
            renderPage(
                createMockPageData(buildFeeTierDetail()),
                createMockRulesPageData(buildRulesCollection(rules)),
            );
            fireEvent.click(screen.getByRole('button', {name: /smazat pravidlo/i}));
            expect(screen.getByRole('dialog', {name: 'Smazat pravidlo'})).toBeInTheDocument();
        });

        it('closes removeRule modal when close button is clicked', () => {
            const rules: PaymentRuleItem[] = [{
                eventTypeId: 'sprint',
                rankingShortName: 'A',
                ruleType: 'PERCENTAGE',
                percentage: 50,
                _templates: {
                    removeRule: mockHalFormsTemplate({title: 'Smazat pravidlo', method: 'DELETE'}),
                },
            }];
            renderPage(
                createMockPageData(buildFeeTierDetail()),
                createMockRulesPageData(buildRulesCollection(rules)),
            );
            fireEvent.click(screen.getByRole('button', {name: /smazat pravidlo/i}));
            fireEvent.click(screen.getByRole('button', {name: /zavřít/i}));
            expect(screen.queryByRole('dialog', {name: 'Smazat pravidlo'})).not.toBeInTheDocument();
        });
    });
});
