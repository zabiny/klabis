import '@testing-library/jest-dom';
import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClientProvider} from '@tanstack/react-query';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembershipFeeTierDetailPage} from './MembershipFeeTierDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {HalRouteContext, type HalRouteContextValue} from '../../contexts/halRouteContext.ts';
import {ToastProvider} from '../../contexts/ToastContext.tsx';
import {halRouteValue, stubFetch, ToastProbe, createTestQueryClient, LocationProbe} from '../../__mocks__/halDialogTest.tsx';

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../hooks/useRootNavigation', () => ({
    useRootNavigation: vi.fn().mockReturnValue({data: [], isLoading: false, error: null}),
}));

let rulesRoute: HalRouteContextValue;

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalSubresourceProvider: ({subresourceLinkName, children}: {subresourceLinkName: string; children: React.ReactNode}) => (
        <div data-testid={`subresource-${subresourceLinkName}`}>
            <HalRouteContext.Provider value={rulesRoute}>{children}</HalRouteContext.Provider>
        </div>
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

type RouteOverrides = Partial<Pick<HalRouteContextValue, 'isLoading' | 'error'>>;

const createMockPageData = (resourceData: HalResponse | null, overrides?: RouteOverrides): HalRouteContextValue => ({
    ...halRouteValue(resourceData, '/administration/membership-fee-tiers/tier-1'),
    ...overrides,
});

const createMockRulesPageData = (rulesCollection: HalResponse | null, overrides?: RouteOverrides): HalRouteContextValue => ({
    ...halRouteValue(rulesCollection, '/administration/membership-fee-tiers/tier-1/rules'),
    ...overrides,
});

const renderPage = (tierData: HalRouteContextValue, rulesData?: HalRouteContextValue) => {
    rulesRoute = rulesData ?? createMockRulesPageData(buildRulesCollection([]));
    const queryClient = createTestQueryClient();
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/administration/membership-fee-tiers/tier-1']}>
                <ToastProvider>
                    <HalRouteContext.Provider value={tierData}>
                        <MembershipFeeTierDetailPage/>
                    </HalRouteContext.Provider>
                    <ToastProbe/>
                    <LocationProbe/>
                </ToastProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MembershipFeeTierDetailPage', () => {
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

    beforeEach(() => {
        vi.clearAllMocks();
        ({writeCalls} = stubFetch());
    });

    afterEach(() => {
        vi.unstubAllGlobals();
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
        expect(screen.getByRole('link', {name: 'Členské příspěvky'})).toHaveAttribute('href', '/membership-fee-tiers');
    });

    it('renders co-participation rules section heading always', () => {
        renderPage(createMockPageData(buildFeeTierDetail()));
        expect(screen.getByText('PRAVIDLA SPOLUÚČASTI')).toBeInTheDocument();
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
        const addRuleTemplate = mockHalFormsTemplate({
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
        });
        const rules: PaymentRuleItem[] = [{
            eventTypeId: 'sprint',
            rankingShortName: '2',
            ruleType: 'PERCENTAGE',
            percentage: 50,
        }];
        const rulesCollection = buildRulesCollection(rules);
        const rulesCollectionWithTemplate = {...rulesCollection, _templates: {addRule: addRuleTemplate}};
        renderPage(
            createMockPageData(buildFeeTierDetail()),
            createMockRulesPageData(rulesCollectionWithTemplate),
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

    it('renders save button inside basic info card when editTier template exists (after clicking edit)', async () => {
        const resourceData = buildFeeTierDetail({
            _templates: {
                editTier: mockHalFormsTemplate({title: 'Upravit tier', method: 'PATCH'}),
            },
        });
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /upravit/i}));
        expect(await screen.findByRole('button', {name: /uložit změny/i})).toBeInTheDocument();
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
        const rulesCollection = buildRulesCollection([]);
        const rulesCollectionWithTemplate = {
            ...rulesCollection,
            _templates: {addRule: mockHalFormsTemplate({title: 'Přidat pravidlo', method: 'POST'})},
        };
        renderPage(
            createMockPageData(buildFeeTierDetail()),
            createMockRulesPageData(rulesCollectionWithTemplate),
        );
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

        it('opens editRule modal when edit button is clicked', async () => {
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
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Upravit pravidlo');
        });

        it('closes editRule modal when cancel is clicked', async () => {
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
            fireEvent.click(await screen.findByRole('button', {name: /zrušit/i}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
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

        it('opens removeRule modal when delete button is clicked', async () => {
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
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Smazat pravidlo');
        });

        it('closes removeRule modal when cancel is clicked', async () => {
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
            fireEvent.click(await screen.findByRole('button', {name: /zrušit/i}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        });
    });

    describe('dialog submits', () => {
        const ruleWith = (templates: Record<string, unknown>): PaymentRuleItem => ({
            eventTypeId: 'sprint',
            rankingShortName: 'A',
            ruleType: 'PERCENTAGE',
            percentage: 50,
            _templates: templates,
        });

        it('submits editRule to the rule self link and shows the default toast', async () => {
            const user = userEvent.setup();
            renderPage(
                createMockPageData(buildFeeTierDetail()),
                createMockRulesPageData(buildRulesCollection([ruleWith({
                    editRule: mockHalFormsTemplate({title: 'Upravit pravidlo', method: 'PATCH', target: undefined, properties: []}),
                })])),
            );

            await user.click(screen.getByRole('button', {name: /upravit pravidlo/i}));
            await user.click(await screen.findByRole('button', {name: /odeslat/i}));

            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/membership-fee-tiers/tier-1/rules/sprint/A');
            expect((writeCalls()[0][1] as RequestInit).method).toBe('PATCH');
            await waitFor(() => expect(screen.getByTestId('toast')).toHaveTextContent('Upravit pravidlo — úspěšně uloženo'));
            expect(screen.getByTestId('location')).toHaveTextContent('/administration/membership-fee-tiers/tier-1');
        });

        it('submits removeRule to the rule self link', async () => {
            const user = userEvent.setup();
            renderPage(
                createMockPageData(buildFeeTierDetail()),
                createMockRulesPageData(buildRulesCollection([ruleWith({
                    removeRule: mockHalFormsTemplate({title: 'Smazat pravidlo', method: 'DELETE', target: undefined, properties: []}),
                })])),
            );

            await user.click(screen.getByRole('button', {name: /smazat pravidlo/i}));
            await user.click(await screen.findByRole('button', {name: /odeslat/i}));

            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/membership-fee-tiers/tier-1/rules/sprint/A');
            expect((writeCalls()[0][1] as RequestInit).method).toBe('DELETE');
        });

        it('opens addRule with the template title and submits to the collection', async () => {
            const user = userEvent.setup();
            const collection = {
                ...buildRulesCollection([]),
                _templates: {
                    addRule: mockHalFormsTemplate({title: 'Přidat pravidlo', method: 'POST', target: undefined, properties: []}),
                },
            };
            renderPage(createMockPageData(buildFeeTierDetail()), createMockRulesPageData(collection));

            await user.click(screen.getByRole('button', {name: /přidat pravidlo/i}));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Přidat pravidlo');
            await user.click(screen.getByRole('button', {name: /odeslat/i}));

            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/membership-fee-tiers/tier-1/rules');
        });

        it('deleting the tier opens the template title dialog and navigates to the tier list', async () => {
            const user = userEvent.setup();
            renderPage(createMockPageData(buildFeeTierDetail({
                _templates: {
                    deleteTier: mockHalFormsTemplate({title: 'Smazat tier', method: 'DELETE', target: undefined, properties: []}),
                },
            })));

            await user.click(screen.getByRole('button', {name: /smazat tier/i}));
            expect(await screen.findByTestId('modal-title')).toHaveTextContent('Smazat tier');
            await user.click(screen.getByRole('button', {name: /odeslat/i}));

            await waitFor(() => expect(writeCalls()).toHaveLength(1));
            expect(writeCalls()[0][0]).toBe('/api/membership-fee-tiers/tier-1');
            await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/membership-fee-tiers'));
        });
    });
});
