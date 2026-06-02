import '@testing-library/jest-dom';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import type {UseHalPageDataReturn} from '../../hooks/useHalPageData';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {EventsPage} from './EventsPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {useAuth} from '../../contexts/AuthContext2';
import {labels} from '../../localization';
import {useOrisEventImport} from '../../hooks/useOrisEventImport';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/AuthContext2', () => ({
    useAuth: vi.fn().mockReturnValue({
        getUser: () => ({memberId: 'M001', firstName: 'Jana', lastName: 'Novak', id: 1, userName: 'ZBM9500'}),
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
        isLoading: false,
    }),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../components/events/ImportOrisEventModal', () => ({
    ImportOrisEventModal: ({isOpen, onClose}: {isOpen: boolean; onClose: () => void}) =>
        isOpen ? (
            <div data-testid="import-oris-modal">
                <span>Import akce z ORIS</span>
                <button onClick={onClose}>Zavřít</button>
            </div>
        ) : null,
}));

vi.mock('../../components/events/BulkSyncOrisModal', () => ({
    BulkSyncOrisModal: ({isOpen, onClose}: {isOpen: boolean; onClose: () => void}) =>
        isOpen ? (
            <div data-testid="bulk-sync-oris-modal">
                <span>Synchronizuji...</span>
                <button onClick={onClose}>Zavřít</button>
            </div>
        ) : null,
}));

vi.mock('../../hooks/useOrisEventImport', () => ({
    useOrisEventImport: vi.fn().mockReturnValue({
        events: [],
        fetchState: 'loading',
        selectedRegion: 'JIHOMORAVSKA',
        isSubmitting: false,
        submitError: null,
        onRegionChange: vi.fn(),
        selectedIds: new Set<number>(),
        onToggleId: vi.fn(),
        onToggleAll: vi.fn(),
        onImportBatch: vi.fn(),
        importResult: null,
        isAllSelected: false,
        isSomeSelected: false,
        canSubmit: false,
        selectionLimit: 50,
        isSelectionLimitReached: false,
    }),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

vi.mock('../../api/authorizedFetch', () => ({
    authorizedFetch: vi.fn(),
    FetchError: class FetchError extends Error {
        public responseStatus: number;
        constructor(message: string, status: number) {
            super(message);
            this.responseStatus = status;
        }
    },
}));

const createMockPageData = (resourceData: HalResponse | null, overrides?: Partial<UseHalPageDataReturn>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/events',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/events'}),
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

const renderPage = (pageData: UseHalPageDataReturn, initialPath = '/events') => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialPath]}>
                <EventsPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
};

describe('EventsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(useAuth).mockReturnValue({
            getUser: () => ({memberId: 'M001', firstName: 'Jana', lastName: 'Novak', id: 1, userName: 'ZBM9500'}),
            isAuthenticated: true,
            login: vi.fn(),
            logout: vi.fn(),
            isLoading: false,
        });
    });

    it('renders page title "Akce"', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByRole('heading', {name: 'Akce'})).toBeInTheDocument();
    });

    describe('deadlines column (6.4)', () => {
        beforeEach(() => {
            vi.useFakeTimers();
            vi.setSystemTime(new Date('2025-05-09'));
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        const buildEventRow = (overrides: Record<string, unknown> = {}) => ({
            id: 'evt-1',
            name: 'Jarní závod',
            eventDate: '2025-04-15',
            status: 'ACTIVE',
            _links: {self: {href: '/api/events/1'}},
            ...overrides,
        });

        const renderWithEvents = (rows: unknown[]) => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {
                    _links: {self: {href: '/api/events'}},
                    _embedded: {eventSummaryDtoList: rows},
                    page: {totalElements: rows.length, totalPages: 1, size: 10, number: 0},
                },
                isLoading: false,
                error: null,
            } as unknown as ReturnType<typeof useAuthorizedQuery>);
            return renderPage(createMockPageData({
                _links: {self: {href: '/api/events'}},
            }));
        };

        it('shows Uzávěrka column header', () => {
            renderWithEvents([buildEventRow()]);
            expect(screen.getByRole('columnheader', {name: /uzávěrka/i})).toBeInTheDocument();
        });

        it('shows formatted deadline from deadlines[0] when one deadline present', () => {
            renderWithEvents([buildEventRow({deadlines: ['2025-03-15']})]);
            expect(screen.getByText('15. 3. 2025')).toBeInTheDocument();
        });

        it('shows the active deadline ordinal badge when multiple deadlines present', () => {
            // all three in the past on 2025-05-09 → active is the last (3rd), no tooltip
            renderWithEvents([buildEventRow({deadlines: ['2025-03-01', '2025-04-01', '2025-04-10']})]);
            expect(screen.getByText('10. 4. 2025')).toBeInTheDocument();
            expect(screen.getByText('3. termín')).toBeInTheDocument();
        });

        it('lists future deadlines in the tooltip when later deadlines remain', () => {
            // active is the 1st deadline; 2nd and 3rd are still in the future
            renderWithEvents([buildEventRow({deadlines: ['2025-05-20', '2025-06-01', '2025-06-15']})]);
            expect(screen.getByText('20. 5. 2025')).toBeInTheDocument();
            expect(screen.getByText('1. termín')).toBeInTheDocument();
            fireEvent.mouseEnter(screen.getByText('1. termín'));
            const tooltip = screen.getByRole('tooltip');
            expect(tooltip).toHaveTextContent('2. termín: 1. 6. 2025');
            expect(tooltip).toHaveTextContent('3. termín: 15. 6. 2025');
        });

        it('shows no badge for a single-deadline event', () => {
            renderWithEvents([buildEventRow({deadlines: ['2025-05-20']})]);
            expect(screen.getByText('20. 5. 2025')).toBeInTheDocument();
            expect(screen.queryByText(/termín/)).not.toBeInTheDocument();
        });

        it('shows first future deadline as relevant when first deadline has already passed', () => {
            renderWithEvents([buildEventRow({deadlines: ['2020-01-01', '2099-12-31']})]);
            expect(screen.getByText('31. 12. 2099')).toBeInTheDocument();
        });

        it('shows last deadline as relevant when all deadlines have passed', () => {
            renderWithEvents([buildEventRow({deadlines: ['2020-01-01', '2021-06-01']})]);
            expect(screen.getByText('1. 6. 2021')).toBeInTheDocument();
        });

        it('shows first deadline (index 0) for single deadline event', () => {
            renderWithEvents([buildEventRow({deadlines: ['2025-03-15']})]);
            expect(screen.getByText('15. 3. 2025')).toBeInTheDocument();
        });
    });

    describe('cancel event action modal', () => {
        const buildEventWithCancelTemplate = () => ({
            id: 'evt-cancel',
            name: 'Zrušitelná akce',
            eventDate: '2025-06-15',
            status: 'ACTIVE',
            deadlines: ['2025-05-01'],
            _links: {self: {href: '/api/events/cancel-evt'}},
            _templates: {
                cancelEvent: mockHalFormsTemplate({
                    method: 'POST',
                    target: '/api/events/cancel-evt/cancel',
                    title: undefined,
                    properties: [{name: 'cancellationReason', type: 'textarea', required: false}],
                }),
            },
        });

        const renderWithCancelableEvent = () => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {
                    _links: {self: {href: '/api/events'}},
                    _embedded: {eventSummaryDtoList: [buildEventWithCancelTemplate()]},
                    page: {totalElements: 1, totalPages: 1, size: 10, number: 0},
                },
                isLoading: false,
                error: null,
            } as unknown as ReturnType<typeof useAuthorizedQuery>);
            return renderPage(createMockPageData({
                _links: {self: {href: '/api/events'}},
            }));
        };

        it('shows localized dialog title when backend does not send template.title', async () => {
            const user = userEvent.setup();
            renderWithCancelableEvent();

            const cancelButton = await screen.findByTitle(labels.templates.cancelEvent);
            await user.click(cancelButton);

            expect(screen.getByTestId('modal-title')).toHaveTextContent('Zrušení akce');
        });
    });

    describe('"Importovat z ORIS" button', () => {
        it('shows button when importEventsBatch template exists in HAL response', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {
                    importEventsBatch: mockHalFormsTemplate({
                        method: 'POST',
                        target: '/api/events/import-batch',
                        title: 'Importovat z ORIS',
                    }),
                },
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.getByRole('button', {name: /importovat z oris/i})).toBeInTheDocument();
        });

        it('prefers importEventsBatch over importEvent when both are present', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {
                    importEventsBatch: mockHalFormsTemplate({
                        method: 'POST',
                        target: '/api/events/import-batch',
                        title: 'Importovat z ORIS',
                    }),
                    importEvent: mockHalFormsTemplate({
                        method: 'POST',
                        target: '/api/events/import',
                        title: 'Importovat z ORIS',
                    }),
                },
            };
            renderPage(createMockPageData(resourceData));
            // Only one button rendered (not duplicated)
            expect(screen.getAllByRole('button', {name: /importovat z oris/i})).toHaveLength(1);
        });

        it('shows button when importEvent template exists in HAL response', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {
                    importEvent: mockHalFormsTemplate({
                        method: 'POST',
                        target: '/api/events/import',
                        title: 'Importovat z ORIS',
                    }),
                },
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.getByRole('button', {name: /importovat z oris/i})).toBeInTheDocument();
        });

        it('does NOT show button when importEvent template is absent', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {
                    createEvent: mockHalFormsTemplate({method: 'POST', title: 'Přidat závod'}),
                },
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.queryByRole('button', {name: /importovat z oris/i})).not.toBeInTheDocument();
        });

        it('does NOT show button when no templates exist', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.queryByRole('button', {name: /importovat z oris/i})).not.toBeInTheDocument();
        });

        it('opens ImportOrisEventModal when button is clicked', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {
                    importEvent: mockHalFormsTemplate({
                        method: 'POST',
                        target: '/api/events/import',
                        title: 'Importovat z ORIS',
                    }),
                },
            };
            renderPage(createMockPageData(resourceData));

            await user.click(screen.getByRole('button', {name: /importovat z oris/i}));

            expect(screen.getByText('Import akce z ORIS')).toBeInTheDocument();
        });

        it('passes the full template object (not just href) to useOrisEventImport', () => {
            const template = mockHalFormsTemplate({
                method: 'POST',
                target: '/api/events/import-batch',
                title: 'Importovat z ORIS',
                properties: [{name: 'orisIds', type: 'number', multi: true, max: 10}],
            });
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {importEventsBatch: template},
            };
            renderPage(createMockPageData(resourceData));

            expect(vi.mocked(useOrisEventImport)).toHaveBeenCalledWith(
                expect.objectContaining({target: '/api/events/import-batch', method: 'POST'}),
                expect.any(Boolean),
                expect.any(Object),
            );
        });
    });

    describe('new-registration prefill flow', () => {
        const newRegistrationUrl = '/api/events/evt-1/registrations/M001?newRegistration=true';

        const registerForEventTarget = '/api/events/evt-1/registrations';

        const buildEventWithNewRegistrationLink = (overrides: Record<string, unknown> = {}) => ({
            id: 'evt-1',
            name: 'Jarní závod',
            eventDate: '2025-06-15',
            status: 'ACTIVE',
            _links: {
                self: {href: '/api/events/evt-1'},
                'newRegistration': {href: newRegistrationUrl},
            },
            _templates: {
                registerForEvent: mockHalFormsTemplate({
                    method: 'POST',
                    target: registerForEventTarget,
                    title: 'Přihlásit se',
                    properties: [
                        {name: 'siCardNumber', prompt: 'SI číslo', type: 'text'},
                        {name: 'category', prompt: 'Kategorie', type: 'text'},
                    ],
                }),
            },
            ...overrides,
        });

        const buildNewRegistrationResponse = (siCardNumber?: string) => ({
            siCardNumber: siCardNumber ?? '',
            category: '',
            _links: {self: {href: newRegistrationUrl}},
            _templates: {
                editRegistration: mockHalFormsTemplate({
                    method: 'PUT',
                    target: '/api/events/evt-1/registrations/M001',
                    title: 'Přihlásit se',
                    properties: [
                        {name: 'siCardNumber', prompt: 'SI číslo', type: 'text', value: siCardNumber ?? ''},
                        {name: 'category', prompt: 'Kategorie', type: 'text'},
                    ],
                }),
            },
        });

        const renderWithEventHavingNewRegistrationLink = (siCardNumber?: string) => {
            vi.mocked(useAuthorizedQuery).mockImplementation((url: string, options?: {enabled?: boolean}) => {
                if (options?.enabled === false) {
                    return {data: undefined, isLoading: false, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>;
                }
                if (url.includes('newRegistration=true')) {
                    return {data: buildNewRegistrationResponse(siCardNumber), isLoading: false, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>;
                }
                return {
                    data: {
                        _links: {self: {href: '/api/events'}},
                        _embedded: {eventSummaryDtoList: [buildEventWithNewRegistrationLink()]},
                        page: {totalElements: 1, totalPages: 1, size: 10, number: 0},
                    },
                    isLoading: false,
                    error: null,
                } as unknown as ReturnType<typeof useAuthorizedQuery>;
            });
            return renderPage(createMockPageData({
                _links: {self: {href: '/api/events'}},
            }));
        };

        it('shows "Přihlásit se" button when event has new-registration link', async () => {
            renderWithEventHavingNewRegistrationLink('12345');
            expect(await screen.findByTitle(labels.templates.registerForEvent)).toBeInTheDocument();
        });

        it('opens registration form prefilled with siCardNumber when button clicked', async () => {
            const user = userEvent.setup();
            renderWithEventHavingNewRegistrationLink('12345');

            const registerBtn = await screen.findByTitle(labels.templates.registerForEvent);
            await user.click(registerBtn);

            const siInput = await screen.findByDisplayValue('12345');
            expect(siInput).toBeInTheDocument();
        });

        it('opens registration form with empty SI field when member has no chip number', async () => {
            const user = userEvent.setup();
            renderWithEventHavingNewRegistrationLink('');

            const registerBtn = await screen.findByTitle(labels.templates.registerForEvent);
            await user.click(registerBtn);

            await screen.findByTestId('hal-forms-display');
            const siInput = document.querySelector('input[name="siCardNumber"]') as HTMLInputElement | null;
            expect(siInput).toBeInTheDocument();
            expect(siInput).toHaveValue('');
        });

        it('submits overwritten siCardNumber value', async () => {
            const user = userEvent.setup();
            const mutateMock = vi.fn();
            vi.mocked(useAuthorizedQuery).mockImplementation((url: string, options?: {enabled?: boolean}) => {
                if (options?.enabled === false) {
                    return {data: undefined, isLoading: false, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>;
                }
                if (url.includes('newRegistration=true')) {
                    return {data: buildNewRegistrationResponse('12345'), isLoading: false, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>;
                }
                return {
                    data: {
                        _links: {self: {href: '/api/events'}},
                        _embedded: {eventSummaryDtoList: [buildEventWithNewRegistrationLink()]},
                        page: {totalElements: 1, totalPages: 1, size: 10, number: 0},
                    },
                    isLoading: false,
                    error: null,
                } as unknown as ReturnType<typeof useAuthorizedQuery>;
            });
            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: mutateMock,
                isPending: false,
                error: null,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderPage(createMockPageData({_links: {self: {href: '/api/events'}}}));

            const registerBtn = await screen.findByTitle(labels.templates.registerForEvent);
            await user.click(registerBtn);

            const siInput = await screen.findByDisplayValue('12345');
            await user.clear(siInput);
            await user.type(siInput, '99999');

            await user.click(screen.getByRole('button', {name: /odeslat/i}));

            await waitFor(() => {
                expect(mutateMock).toHaveBeenCalledWith(
                    expect.objectContaining({
                        data: expect.objectContaining({siCardNumber: '99999'}),
                    }),
                    expect.anything(),
                );
            });
        });

        it('submits new registration via POST to registerForEvent URL, not PUT to editRegistration', async () => {
            const user = userEvent.setup();
            const mutateMock = vi.fn();
            vi.mocked(useAuthorizedQuery).mockImplementation((url: string, options?: {enabled?: boolean}) => {
                if (options?.enabled === false) {
                    return {data: undefined, isLoading: false, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>;
                }
                if (url.includes('newRegistration=true')) {
                    return {data: buildNewRegistrationResponse('12345'), isLoading: false, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>;
                }
                return {
                    data: {
                        _links: {self: {href: '/api/events'}},
                        _embedded: {eventSummaryDtoList: [buildEventWithNewRegistrationLink()]},
                        page: {totalElements: 1, totalPages: 1, size: 10, number: 0},
                    },
                    isLoading: false,
                    error: null,
                } as unknown as ReturnType<typeof useAuthorizedQuery>;
            });
            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: mutateMock,
                isPending: false,
                error: null,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderPage(createMockPageData({_links: {self: {href: '/api/events'}}}));

            const registerBtn = await screen.findByTitle(labels.templates.registerForEvent);
            await user.click(registerBtn);

            await screen.findByTestId('hal-forms-display');
            await user.click(screen.getByRole('button', {name: /odeslat/i}));

            await waitFor(() => {
                expect(mutateMock).toHaveBeenCalledWith(
                    expect.objectContaining({url: registerForEventTarget}),
                    expect.anything(),
                );
            });
            // Must NOT submit to the editRegistration PUT URL
            expect(mutateMock).not.toHaveBeenCalledWith(
                expect.objectContaining({url: '/api/events/evt-1/registrations/M001'}),
                expect.anything(),
            );
        });
    });

    describe('action button color variants (K2)', () => {
        const buildEventWithTemplates = (templates: Record<string, unknown>) => ({
            id: 'evt-color',
            name: 'Barevná akce',
            eventDate: '2025-09-01',
            status: 'ACTIVE',
            _links: {self: {href: '/api/events/evt-color'}},
            _templates: templates,
        });

        const renderWithEventTemplates = (templates: Record<string, unknown>) => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: {
                    _links: {self: {href: '/api/events'}},
                    _embedded: {eventSummaryDtoList: [buildEventWithTemplates(templates)]},
                    page: {totalElements: 1, totalPages: 1, size: 10, number: 0},
                },
                isLoading: false,
                error: null,
            } as unknown as ReturnType<typeof useAuthorizedQuery>);
            return renderPage(createMockPageData({
                _links: {self: {href: '/api/events'}},
            }));
        };

        it('renders cancelEvent button with danger-ghost (text-red-600) class', async () => {
            renderWithEventTemplates({
                cancelEvent: mockHalFormsTemplate({method: 'POST', target: '/api/events/evt-color/cancel'}),
            });
            const btn = await screen.findByTitle(labels.templates.cancelEvent);
            expect(btn).toHaveClass('text-red-600');
        });

        it('renders registerForEvent button with primary-ghost (text-primary) class', async () => {
            renderWithEventTemplates({
                registerForEvent: mockHalFormsTemplate({method: 'POST', target: '/api/events/evt-color/register'}),
            });
            const btn = await screen.findByTitle(labels.templates.registerForEvent);
            expect(btn).toHaveClass('text-primary');
        });
    });

    describe('filter bar', () => {
        it('renders filter bar above the events table', () => {
            renderPage(createMockPageData(null));
            expect(screen.getByPlaceholderText(labels.eventsFilter.searchPlaceholder)).toBeInTheDocument();
            expect(screen.getByRole('button', {name: labels.eventsFilter.budouci})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: labels.eventsFilter.probehle})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: labels.eventsFilter.vse})).toBeInTheDocument();
        });

        it('shows "Moje přihlášky" checkbox for user with member profile', () => {
            renderPage(createMockPageData(null));
            expect(screen.getByRole('checkbox', {name: labels.eventsFilter.mojePřihlaskyLabel})).toBeInTheDocument();
        });

        it('hides "Moje přihlášky" checkbox for user without member profile', () => {
            vi.mocked(useAuth).mockReturnValue({
                getUser: () => ({memberId: null, firstName: 'Admin', lastName: 'User', id: 2, userName: 'ZBM9000'}),
                isAuthenticated: true,
                login: vi.fn(),
                logout: vi.fn(),
                isLoading: false,
            });
            renderPage(createMockPageData(null));
            expect(screen.queryByRole('checkbox', {name: labels.eventsFilter.mojePřihlaskyLabel})).not.toBeInTheDocument();
        });

        it('shows Budoucí as active time window by default', () => {
            renderPage(createMockPageData(null));
            const budouciBtn = screen.getByRole('button', {name: labels.eventsFilter.budouci});
            expect(budouciBtn).toHaveAttribute('aria-pressed', 'true');
        });

        it('shows Proběhlé as active when ?when=probehle is in URL', () => {
            renderPage(createMockPageData(null), '/events?when=probehle');
            const probehleBtn = screen.getByRole('button', {name: labels.eventsFilter.probehle});
            expect(probehleBtn).toHaveAttribute('aria-pressed', 'true');
        });

        it('shows Vše as active when ?when=vse is in URL', () => {
            renderPage(createMockPageData(null), '/events?year=2024&when=vse');
            const vseBtn = screen.getByRole('button', {name: labels.eventsFilter.vse});
            expect(vseBtn).toHaveAttribute('aria-pressed', 'true');
        });

        it('switches time window to Vše when Vše button is clicked', async () => {
            const user = userEvent.setup();
            renderPage(createMockPageData(null));

            await user.click(screen.getByRole('button', {name: labels.eventsFilter.vse}));

            await waitFor(() => {
                const vseBtn = screen.getByRole('button', {name: labels.eventsFilter.vse});
                expect(vseBtn).toHaveAttribute('aria-pressed', 'true');
            });
        });

        it('reflects search query from URL', () => {
            renderPage(createMockPageData(null), '/events?q=jihlava');
            expect(screen.getByDisplayValue('jihlava')).toBeInTheDocument();
        });

        it('reflects registeredBy=me from URL as checked checkbox', () => {
            renderPage(createMockPageData(null), '/events?registeredBy=me');
            const checkbox = screen.getByRole('checkbox', {name: labels.eventsFilter.mojePřihlaskyLabel});
            expect(checkbox).toBeChecked();
        });

        it('reflects eventTypeId from URL by pressing the matching pill', () => {
            vi.mocked(useAuthorizedQuery).mockImplementation((url: string) => {
                if (url === '/api/event-types') {
                    return {
                        data: {_embedded: {eventTypeDtoList: [
                            {id: 'et-A', name: 'Klub', sortOrder: 1},
                            {id: 'et-B', name: 'Oblastní', sortOrder: 2},
                        ]}},
                        isLoading: false,
                        error: null,
                    } as unknown as ReturnType<typeof useAuthorizedQuery>;
                }
                return {data: null, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>;
            });
            renderPage(createMockPageData(null), '/events?eventTypeId=et-A');
            const group = screen.getByRole('group', {name: labels.eventsFilter.eventTypeFilter});
            const klubBtn = within(group).getByRole('button', {name: 'Klub'});
            expect(klubBtn).toHaveAttribute('aria-pressed', 'true');
        });
    });

    describe('"Synchronizovat všechny budoucí z ORIS" button (A2.1)', () => {
        it('shows button when syncAllUpcomingFromOris template exists in HAL response', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {
                    syncAllUpcomingFromOris: mockHalFormsTemplate({
                        method: 'POST',
                        target: '/api/events/sync-from-oris/all-upcoming',
                        title: 'Synchronizovat všechny budoucí z ORIS',
                    }),
                },
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.getByRole('button', {name: labels.templates.syncAllUpcomingFromOris})).toBeInTheDocument();
        });

        it('does NOT show button when syncAllUpcomingFromOris template is absent', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {
                    createEvent: mockHalFormsTemplate({method: 'POST', title: 'Přidat závod'}),
                },
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.queryByRole('button', {name: labels.templates.syncAllUpcomingFromOris})).not.toBeInTheDocument();
        });

        it('opens BulkSyncOrisModal when button is clicked', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/events'}},
                _templates: {
                    syncAllUpcomingFromOris: mockHalFormsTemplate({
                        method: 'POST',
                        target: '/api/events/sync-from-oris/all-upcoming',
                        title: 'Synchronizovat všechny budoucí z ORIS',
                    }),
                },
            };
            renderPage(createMockPageData(resourceData));

            await user.click(screen.getByRole('button', {name: labels.templates.syncAllUpcomingFromOris}));

            expect(screen.getByTestId('bulk-sync-oris-modal')).toBeInTheDocument();
        });
    });

    describe('event type column (B7.2)', () => {
        const buildEventWithType = (eventTypeId: string | null = null) => ({
            id: 'evt-type-1',
            name: 'Pohárový závod',
            eventDate: '2025-06-01',
            status: 'ACTIVE',
            eventTypeId,
            _links: {self: {href: '/api/events/evt-type-1'}},
        });

        const renderWithEventsAndTypes = (
            events: unknown[],
            eventTypes: {id: string; name: string; color?: string; sortOrder: number}[] = []
        ) => {
            vi.mocked(useAuthorizedQuery).mockImplementation((url: string) => {
                if (url === '/api/event-types') {
                    return {
                        data: {_embedded: {eventTypeDtoList: eventTypes}},
                        isLoading: false,
                        error: null,
                    } as unknown as ReturnType<typeof useAuthorizedQuery>;
                }
                return {
                    data: {
                        _links: {self: {href: '/api/events'}},
                        _embedded: {eventSummaryDtoList: events},
                        page: {totalElements: events.length, totalPages: 1, size: 10, number: 0},
                    },
                    isLoading: false,
                    error: null,
                } as unknown as ReturnType<typeof useAuthorizedQuery>;
            });
            return renderPage(createMockPageData({_links: {self: {href: '/api/events'}}}));
        };

        it('shows "Typ" column header in events table when an event has a type', () => {
            const eventTypes = [{id: 'type-abc', name: 'Trénink', color: '#00FF00', sortOrder: 1}];
            renderWithEventsAndTypes([buildEventWithType('type-abc')], eventTypes);
            expect(screen.getByRole('columnheader', {name: labels.tables.eventType})).toBeInTheDocument();
        });

        it('shows event type name badge when event has eventTypeId matching catalog', () => {
            const eventTypes = [{id: 'type-abc', name: 'Trénink', color: '#00FF00', sortOrder: 1}];
            renderWithEventsAndTypes([buildEventWithType('type-abc')], eventTypes);
            const matches = screen.getAllByText('Trénink');
            const badge = matches.find((el) => el.tagName.toLowerCase() !== 'option');
            expect(badge).toBeInTheDocument();
        });

        it('shows no badge when event has no eventTypeId', () => {
            const eventTypes = [{id: 'type-abc', name: 'Trénink', color: '#00FF00', sortOrder: 1}];
            renderWithEventsAndTypes([buildEventWithType(null)], eventTypes);
            // The name may appear in the event type filter as a pill button, but not as a badge span
            const matches = screen.queryAllByText('Trénink');
            const badge = matches.find((el) => {
                const tag = el.tagName.toLowerCase();
                return tag !== 'option' && tag !== 'button';
            });
            expect(badge).not.toBeDefined();
        });

        it('shows no badge when event has eventTypeId not found in catalog', () => {
            renderWithEventsAndTypes([buildEventWithType('unknown-type-id')], []);
            expect(screen.queryByText('Trénink')).not.toBeInTheDocument();
        });
    });
});
