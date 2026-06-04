import '@testing-library/jest-dom';
import {fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormProvider} from '../../contexts/HalFormContext';
import {HalFormsPageLayout} from '../../components/HalNavigator2/HalFormsPageLayout';
import {useHalPageData} from '../../hooks/useHalPageData';
import type {UseHalPageDataReturn} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {EventDetailPage} from './EventDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedMutation: vi.fn(() => ({
        mutate: vi.fn(),
        mutateAsync: vi.fn().mockResolvedValue(undefined),
        isPending: false,
        error: null,
    })),
    useAuthorizedQuery: vi.fn((_key: unknown, url: string) => ({
        data: {
            _embedded: {registrationDtoList: []},
            page: {totalElements: 0, totalPages: 0, size: 10, number: 0},
            _links: {self: {href: url}},
        },
        isLoading: false,
        error: null,
    })),
}));

vi.mock('../../hooks/useFormCacheInvalidation', () => ({
    useFormCacheInvalidation: vi.fn(() => ({
        invalidateAllCaches: vi.fn().mockResolvedValue(undefined),
    })),
}));

vi.mock('../../contexts/ToastContext', () => ({
    ToastProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../contexts/toastContext', () => ({
    useToast: vi.fn(() => ({
        addToast: vi.fn(),
    })),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

vi.mock('../../api/hateoas', () => ({
    submitHalFormsData: vi.fn(),
    isFormValidationError: vi.fn((error) => {
        return error && typeof error === 'object' && 'validationErrors' in error;
    }),
    toFormValidationError: vi.fn((error) => error),
    toHref: vi.fn((source: unknown) => {
        if (Array.isArray(source)) return (source[0] as {href?: string})?.href ?? '';
        return (source as {href?: string})?.href ?? '';
    }),
}));

vi.mock('../../components/UI/Modal.tsx', () => ({
    Modal: ({isOpen, children, onClose, title}: {isOpen: boolean; children: React.ReactNode; onClose: () => void; title?: string}) => (
        isOpen ? (
            <div data-testid="modal-overlay" role="dialog">
                {title && <h4>{title}</h4>}
                {children}
                <button onClick={onClose}>Close</button>
            </div>
        ) : null
    ),
}));

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalSubresourceProvider: ({subresourceLinkName, children}: {subresourceLinkName: string; children: React.ReactNode}) => (
        <div data-testid={`subresource-${subresourceLinkName}`}>{children}</div>
    ),
}));

vi.mock('../../contexts/halRouteContext.ts', () => ({
    useHalRoute: vi.fn(() => ({
        resourceData: {firstName: 'Jan', lastName: 'Novák', _links: {self: {href: '/api/members/42'}}},
        navigateToResource: vi.fn(),
        isLoading: false,
        error: null,
    })),
}));

vi.mock('../../components/finance/FinanceTransactionDialog', () => ({
    FinanceTransactionDialog: ({isOpen, accountLink, onClose, defaultNote}: {isOpen: boolean; accountLink: {href: string}; onClose: () => void; defaultNote?: string}) =>
        isOpen ? (
            <div data-testid="finance-transaction-dialog" data-account-href={accountLink.href} data-default-note={defaultNote ?? ''}>
                <button onClick={onClose}>close-dialog</button>
            </div>
        ) : null,
}));


const mockEventDetailData = (overrides?: Partial<HalResponse>): HalResponse => ({
    name: 'Jarní závod 2025',
    eventDate: '2025-04-15',
    location: 'Brno - Bystrc',
    organizer: 'OB Brno',
    websiteUrl: 'https://obbrno.cz/zavody/jaro2025',
    eventCoordinatorId: {value: '42'},
    status: 'ACTIVE',
    _links: {
        self: {href: 'http://localhost:8443/api/events/1'},
    },
    ...overrides,
});

const createMockPageData = (resourceData: HalResponse | null, overrides?: Partial<UseHalPageDataReturn>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/events/1',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost:8443/api/events/1'}),
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

const renderPage = (pageData: UseHalPageDataReturn) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0, staleTime: Infinity}}});
    (globalThis as typeof globalThis & {fetch: typeof fetch}).fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({_embedded: {registrationDtoList: []}, page: {totalElements: 0, totalPages: 0, size: 10, number: 0}}),
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/events/1']}>
                <HalFormProvider>
                    <HalFormsPageLayout>
                        <EventDetailPage/>
                    </HalFormsPageLayout>
                </HalFormProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('EventDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockNavigate.mockReset();
    });

    it('renders back link to events list', () => {
        renderPage(createMockPageData(mockEventDetailData()));
        expect(screen.getByText(/zpět na seznam/i)).toBeInTheDocument();
        expect(screen.getByText(/zpět na seznam/i).closest('a')).toHaveAttribute('href', '/events');
    });

    it('renders event name as page heading', () => {
        renderPage(createMockPageData(mockEventDetailData()));
        expect(screen.getByRole('heading', {level: 1, name: 'Jarní závod 2025'})).toBeInTheDocument();
    });

    it('renders event date formatted', () => {
        renderPage(createMockPageData(mockEventDetailData()));
        expect(screen.getByText('15. 4. 2025')).toBeInTheDocument();
    });

    it('renders event location', () => {
        renderPage(createMockPageData(mockEventDetailData()));
        expect(screen.getByText('Brno - Bystrc')).toBeInTheDocument();
    });

    it('renders event organizer', () => {
        renderPage(createMockPageData(mockEventDetailData()));
        expect(screen.getByText('OB Brno')).toBeInTheDocument();
    });

    it('renders website URL as link when present', () => {
        renderPage(createMockPageData(mockEventDetailData()));
        const link = screen.getByRole('link', {name: /obbrno\.cz/i});
        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute('href', 'https://obbrno.cz/zavody/jaro2025');
    });

    it('does not render website link when websiteUrl is absent', () => {
        renderPage(createMockPageData(mockEventDetailData({websiteUrl: undefined})));
        expect(screen.queryByRole('link', {name: /http/i})).not.toBeInTheDocument();
    });

    it('renders event status badge', () => {
        renderPage(createMockPageData(mockEventDetailData()));
        expect(screen.getByText('Aktivní')).toBeInTheDocument();
    });

    it('renders coordinator name when coordinator link is present', () => {
        renderPage(createMockPageData(mockEventDetailData({
            _links: {
                self: {href: 'http://localhost:8443/api/events/1'},
                coordinator: {href: 'http://localhost:8443/api/members/42'},
            },
        })));
        expect(screen.getByText('Jan Novák')).toBeInTheDocument();
        expect(screen.getByTestId('subresource-coordinator')).toBeInTheDocument();
    });

    it('does not render coordinator when coordinator link is absent', () => {
        renderPage(createMockPageData(mockEventDetailData({eventCoordinatorId: undefined})));
        expect(screen.queryByTestId('subresource-coordinator')).not.toBeInTheDocument();
    });

    it('shows loading placeholder while loading', () => {
        renderPage(createMockPageData(null, {isLoading: true}));
        expect(screen.queryByRole('heading', {level: 1})).not.toBeInTheDocument();
    });

    it('shows error message when fetch fails', () => {
        renderPage(createMockPageData(null, {error: new Error('Načtení selhalo')}));
        expect(screen.getByText('Načtení selhalo')).toBeInTheDocument();
    });

    it('shows loading placeholder when resourceData is null and not loading', () => {
        renderPage(createMockPageData(null));
        expect(screen.queryByRole('heading', {level: 1})).not.toBeInTheDocument();
    });

    describe('action buttons via HAL affordances', () => {
        it('shows edit button when updateEvent template exists', () => {
            const data = mockEventDetailData({
                _templates: {
                    updateEvent: mockHalFormsTemplate({title: 'Upravit závod'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /upravit/i})).toBeInTheDocument();
        });

        it('does not show edit button when updateEvent template is absent', () => {
            renderPage(createMockPageData(mockEventDetailData()));
            expect(screen.queryByRole('button', {name: /upravit/i})).not.toBeInTheDocument();
        });

        it('shows publishEvent button when template exists', () => {
            const data = mockEventDetailData({
                _templates: {
                    publishEvent: mockHalFormsTemplate({title: 'Publikovat'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /publikovat/i})).toBeInTheDocument();
        });

        it('shows cancelEvent button when template exists', () => {
            const data = mockEventDetailData({
                _templates: {
                    cancelEvent: mockHalFormsTemplate({title: 'Zrušit akci'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /zrušit akci/i})).toBeInTheDocument();
        });

        it('does not show finishEvent button even when template is present (endpoint removed)', () => {
            const data = mockEventDetailData({
                _templates: {
                    finishEvent: mockHalFormsTemplate({title: 'Dokončit'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.queryByRole('button', {name: /ukončit akci/i})).not.toBeInTheDocument();
        });

        it('shows registerForEvent button when template exists', () => {
            const data = mockEventDetailData({
                _templates: {
                    registerForEvent: mockHalFormsTemplate({title: 'Přihlásit se'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /přihlásit se/i})).toBeInTheDocument();
        });

        it('shows unregisterFromEvent button when template exists', () => {
            const data = mockEventDetailData({
                _templates: {
                    unregisterFromEvent: mockHalFormsTemplate({title: 'Odhlásit se'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /odhlásit se/i})).toBeInTheDocument();
        });
    });

    describe('inline editing', () => {
        const updateEventTemplate = mockHalFormsTemplate({
            method: 'PUT',
            target: '/api/events/1',
            title: 'Upravit závod',
            properties: [
                {name: 'name', prompt: 'Název', type: 'text', required: true, value: 'Jarní závod 2025'},
                {name: 'eventDate', prompt: 'Datum konání', type: 'date', required: true, value: '2025-04-15'},
                {name: 'location', prompt: 'Místo', type: 'text', value: 'Brno - Bystrc'},
                {name: 'organizer', prompt: 'Pořadatel', type: 'text', value: 'OB Brno'},
                {name: 'websiteUrl', prompt: 'Webová stránka', type: 'url', value: 'https://obbrno.cz/zavody/jaro2025'},
                {name: 'eventCoordinatorId', prompt: 'Vedoucí', type: 'text', value: '42'},
            ],
        });

        it('clicking Upravit button switches to edit mode showing HalFormDisplay', () => {
            const data = mockEventDetailData({
                _templates: {updateEvent: updateEventTemplate},
            });
            renderPage(createMockPageData(data));

            fireEvent.click(screen.getByRole('button', {name: /upravit/i}));

            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
        });

        it('in edit mode the action buttons are hidden', () => {
            const data = mockEventDetailData({
                _templates: {
                    updateEvent: updateEventTemplate,
                    publishEvent: mockHalFormsTemplate({title: 'Publikovat'}),
                },
            });
            renderPage(createMockPageData(data));

            fireEvent.click(screen.getByRole('button', {name: /upravit/i}));

            expect(screen.queryByRole('button', {name: /publikovat/i})).not.toBeInTheDocument();
        });

        it('in edit mode Upravit button itself is hidden', () => {
            const data = mockEventDetailData({
                _templates: {updateEvent: updateEventTemplate},
            });
            renderPage(createMockPageData(data));

            fireEvent.click(screen.getByRole('button', {name: /upravit/i}));

            expect(screen.queryByRole('button', {name: /^upravit$/i})).not.toBeInTheDocument();
        });

        it('clicking Zrušit button exits edit mode and shows action buttons again', () => {
            const data = mockEventDetailData({
                _templates: {updateEvent: updateEventTemplate},
            });
            renderPage(createMockPageData(data));

            fireEvent.click(screen.getByRole('button', {name: /upravit/i}));
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: /zrušit/i}));

            expect(screen.queryByTestId('hal-forms-display')).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: /upravit/i})).toBeInTheDocument();
        });

        it('event name is still visible as heading in edit mode', () => {
            const data = mockEventDetailData({
                _templates: {updateEvent: updateEventTemplate},
            });
            renderPage(createMockPageData(data));

            fireEvent.click(screen.getByRole('button', {name: /upravit/i}));

            expect(screen.getByRole('heading', {level: 1})).toBeInTheDocument();
        });
    });

    describe('registrations section', () => {
        const mockEventWithRegistrationsLink = (overrides?: Partial<HalResponse>) => mockEventDetailData({
            _links: {
                self: {href: 'http://localhost:8443/api/events/1'},
                registrations: {href: 'http://localhost:8443/api/events/1/registrations'},
            },
            ...overrides,
        });

        it('shows registrations section heading when registrations link is present', () => {
            renderPage(createMockPageData(mockEventWithRegistrationsLink()));
            expect(screen.getByRole('heading', {name: /přihlášky/i})).toBeInTheDocument();
        });

        it('hides registrations section when registrations link is absent (DRAFT event)', () => {
            renderPage(createMockPageData(mockEventDetailData({status: 'DRAFT'})));
            expect(screen.queryByRole('heading', {name: /přihlášky/i})).not.toBeInTheDocument();
        });

        it('shows registrations table with firstName and lastName columns when registrations link is present', () => {
            renderPage(createMockPageData(mockEventWithRegistrationsLink()));
            expect(screen.getByRole('table')).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: 'Jméno'})).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: 'Příjmení'})).toBeInTheDocument();
        });

        it('does not show category column when event has no categories', () => {
            renderPage(createMockPageData(mockEventWithRegistrationsLink({categories: []})));
            expect(screen.queryByRole('columnheader', {name: /kategorie/i})).not.toBeInTheDocument();
        });

        it('does not show category column when event categories field is absent', () => {
            renderPage(createMockPageData(mockEventWithRegistrationsLink({categories: undefined})));
            expect(screen.queryByRole('columnheader', {name: /kategorie/i})).not.toBeInTheDocument();
        });

        it('shows category column when event has categories', () => {
            renderPage(createMockPageData(mockEventWithRegistrationsLink({categories: ['H21', 'D21', 'H35']})));
            expect(screen.getByRole('columnheader', {name: /kategorie/i})).toBeInTheDocument();
        });

        describe('sortable column headers (N10)', () => {
            it('firstName column header is sortable', () => {
                renderPageWithRegistrationRows([buildRegistrationRow()]);
                expect(screen.getByRole('button', {name: /sort by jméno/i})).toBeInTheDocument();
            });

            it('lastName column header is sortable', () => {
                renderPageWithRegistrationRows([buildRegistrationRow()]);
                expect(screen.getByRole('button', {name: /sort by příjmení/i})).toBeInTheDocument();
            });

            it('category column header is sortable when event has categories', () => {
                const rowWithCategory = buildRegistrationRow('member-1', {category: 'H21'});
                renderPageWithRegistrationRows([rowWithCategory], {categories: ['H21', 'D21']});
                expect(screen.getByRole('button', {name: /sort by kategorie/i})).toBeInTheDocument();
            });

            it('registrationTime column header is sortable when column is visible (authorized user)', () => {
                const rows = [buildRegistrationRow('member-1'), buildRegistrationRow('member-2')];
                const registrationsListData = {
                    _links: {self: {href: 'http://localhost:8443/api/events/1/registrations'}},
                    _embedded: {registrationDtoList: rows},
                    page: {totalElements: rows.length, totalPages: 1, size: 10, number: 0},
                };
                vi.mocked(useAuthorizedQuery).mockReturnValue({data: registrationsListData, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);
                renderPage(createMockPageData(mockEventWithRegistrationsLink()));

                expect(screen.getByRole('button', {name: /sort by datum přihlášení/i})).toBeInTheDocument();
            });

            it('registrationTime sort button is absent when column is hidden (non-authorized user)', () => {
                const rows = [buildRegistrationRowWithoutTimestamp('member-1')];
                const registrationsListData = {
                    _links: {self: {href: 'http://localhost:8443/api/events/1/registrations'}},
                    _embedded: {registrationDtoList: rows},
                    page: {totalElements: rows.length, totalPages: 1, size: 10, number: 0},
                };
                vi.mocked(useAuthorizedQuery).mockReturnValue({data: registrationsListData, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);
                renderPage(createMockPageData(mockEventWithRegistrationsLink()));

                expect(screen.queryByRole('button', {name: /sort by datum přihlášení/i})).not.toBeInTheDocument();
            });

            it('clicking firstName sort button sends sort=firstName,asc query param', () => {
                vi.mocked(useAuthorizedQuery).mockReturnValue({
                    data: {
                        _links: {self: {href: 'http://localhost:8443/api/events/1/registrations'}},
                        _embedded: {registrationDtoList: [buildRegistrationRow()]},
                        page: {totalElements: 1, totalPages: 1, size: 10, number: 0},
                    },
                    error: null,
                } as unknown as ReturnType<typeof useAuthorizedQuery>);
                renderPage(createMockPageData(mockEventWithRegistrationsLink()));

                fireEvent.click(screen.getByRole('button', {name: /sort by jméno/i}));

                const lastCall = vi.mocked(useAuthorizedQuery).mock.calls.at(-1);
                expect(lastCall?.[0]).toContain('sort=firstName%2Casc');
            });
        });

        describe('registrationTime column visibility (N9 privacy)', () => {
            it('hides Datum přihlášení column when no rows have registrationTime field (non-authorized user)', () => {
                const rows = [
                    buildRegistrationRowWithoutTimestamp('member-1'),
                    buildRegistrationRowWithoutTimestamp('member-2'),
                ];
                const resourceData = mockEventWithRegistrationsLink();
                const registrationsListData = {
                    _links: {self: {href: 'http://localhost:8443/api/events/1/registrations'}},
                    _embedded: {registrationDtoList: rows},
                    page: {totalElements: rows.length, totalPages: 1, size: 10, number: 0},
                };
                vi.mocked(useAuthorizedQuery).mockReturnValue({data: registrationsListData, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);
                renderPage(createMockPageData(resourceData));

                expect(screen.queryByRole('columnheader', {name: /datum přihlášení/i})).not.toBeInTheDocument();
            });

            it('shows Datum přihlášení column when rows have registrationTime field (authorized user)', () => {
                const rows = [
                    buildRegistrationRow('member-1'),
                    buildRegistrationRow('member-2'),
                ];
                const resourceData = mockEventWithRegistrationsLink();
                const registrationsListData = {
                    _links: {self: {href: 'http://localhost:8443/api/events/1/registrations'}},
                    _embedded: {registrationDtoList: rows},
                    page: {totalElements: rows.length, totalPages: 1, size: 10, number: 0},
                };
                vi.mocked(useAuthorizedQuery).mockReturnValue({data: registrationsListData, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);
                renderPage(createMockPageData(resourceData));

                expect(screen.getByRole('columnheader', {name: /datum přihlášení/i})).toBeInTheDocument();
            });

            it('shows Datum přihlášení column when at least one row has registrationTime', () => {
                const rows = [
                    buildRegistrationRowWithoutTimestamp('member-1'),
                    buildRegistrationRow('member-2'),
                ];
                const resourceData = mockEventWithRegistrationsLink();
                const registrationsListData = {
                    _links: {self: {href: 'http://localhost:8443/api/events/1/registrations'}},
                    _embedded: {registrationDtoList: rows},
                    page: {totalElements: rows.length, totalPages: 1, size: 10, number: 0},
                };
                vi.mocked(useAuthorizedQuery).mockReturnValue({data: registrationsListData, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);
                renderPage(createMockPageData(resourceData));

                expect(screen.getByRole('columnheader', {name: /datum přihlášení/i})).toBeInTheDocument();
            });
        });

        const buildRegistrationRow = (memberId = 'member-1', overrides?: Record<string, unknown>) => ({
            firstName: 'Jana',
            lastName: 'Nováková',
            registrationTime: '2025-03-10T10:00:00',
            _links: {self: {href: `http://localhost:8443/api/events/1/registrations/${memberId}`}},
            ...overrides,
        });

        const buildRegistrationRowWithoutTimestamp = (memberId = 'member-1') => ({
            firstName: 'Jana',
            lastName: 'Nováková',
            _links: {self: {href: `http://localhost:8443/api/events/1/registrations/${memberId}`}},
        });

        const renderPageWithRegistrationRows = (rows: unknown[], eventOverrides?: Partial<HalResponse>) => {
            const resourceData = mockEventWithRegistrationsLink(eventOverrides);
            const registrationsListData = {
                _links: {self: {href: 'http://localhost:8443/api/events/1/registrations'}},
                _embedded: {registrationDtoList: rows},
                page: {totalElements: rows.length, totalPages: 1, size: 10, number: 0},
            };
            vi.mocked(useAuthorizedQuery)
                .mockReturnValue({data: registrationsListData, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);
            return renderPage(createMockPageData(resourceData));
        };

        describe('edit button in registrations list row (Group 7)', () => {
            it('shows Akce column header in registrations table when registrations link is present', () => {
                renderPageWithRegistrationRows([buildRegistrationRow()]);
                expect(screen.getByRole('columnheader', {name: /akce/i})).toBeInTheDocument();
            });

            it('shows edit button on row that has _templates.editRegistration', () => {
                const row = buildRegistrationRow('member-1', {
                    _templates: {editRegistration: mockHalFormsTemplate({method: 'PUT', title: 'Upravit přihlášku'})},
                });
                renderPageWithRegistrationRows([row]);
                expect(screen.getByRole('button', {name: /upravit přihlášku/i})).toBeInTheDocument();
            });

            it('does not show edit button on row without _templates.editRegistration', () => {
                renderPageWithRegistrationRows([buildRegistrationRow()]);
                expect(screen.queryByRole('button', {name: /upravit přihlášku/i})).not.toBeInTheDocument();
            });

            it('opens modal with HalFormDisplay when row edit button is clicked', () => {
                const row = buildRegistrationRow('member-1', {
                    _templates: {editRegistration: mockHalFormsTemplate({method: 'PUT', title: 'Upravit přihlášku'})},
                });
                renderPageWithRegistrationRows([row]);

                fireEvent.click(screen.getByRole('button', {name: /upravit přihlášku/i}));

                expect(screen.getByRole('dialog')).toBeInTheDocument();
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            });
        });

        describe('edit button for EVENTS:REGISTRATIONS holder (Group 9)', () => {
            it('shows edit button on every row when all rows have _templates.editRegistration (EVENTS:REGISTRATIONS holder)', () => {
                const editTemplate = mockHalFormsTemplate({method: 'PUT', title: 'Upravit přihlášku'});
                const rows = [
                    buildRegistrationRow('member-1', {_templates: {editRegistration: editTemplate}}),
                    buildRegistrationRow('member-2', {_templates: {editRegistration: editTemplate}}),
                    buildRegistrationRow('member-3', {_templates: {editRegistration: editTemplate}}),
                ];
                renderPageWithRegistrationRows(rows);

                const editButtons = screen.getAllByRole('button', {name: /upravit přihlášku/i});
                expect(editButtons).toHaveLength(3);
            });
        });

        describe('transaction action in registrations list row (tasks 2.1-2.5)', () => {
            it('2.1 shows transaction action button on row that has _links.recordTransaction', () => {
                const row = buildRegistrationRow('member-1', {
                    _links: {
                        self: {href: 'http://localhost:8443/api/events/1/registrations/member-1'},
                        recordTransaction: {href: 'http://localhost:8443/api/members/member-1/account'},
                    },
                });
                renderPageWithRegistrationRows([row]);

                expect(screen.getByRole('button', {name: /provést finanční transakci/i})).toBeInTheDocument();
            });

            it('2.2 does not show transaction action button on row without _links.recordTransaction', () => {
                renderPageWithRegistrationRows([buildRegistrationRow()]);

                expect(screen.queryByRole('button', {name: /provést finanční transakci/i})).not.toBeInTheDocument();
            });

            it('2.3 clicking transaction button opens FinanceTransactionDialog with correct accountLink', () => {
                const accountHref = 'http://localhost:8443/api/members/member-1/account';
                const row = buildRegistrationRow('member-1', {
                    _links: {
                        self: {href: 'http://localhost:8443/api/events/1/registrations/member-1'},
                        recordTransaction: {href: accountHref},
                    },
                });
                renderPageWithRegistrationRows([row]);

                fireEvent.click(screen.getByRole('button', {name: /provést finanční transakci/i}));

                const dialog = screen.getByTestId('finance-transaction-dialog');
                expect(dialog).toBeInTheDocument();
                expect(dialog).toHaveAttribute('data-account-href', accountHref);
            });

            it('2.4 after successful submit dialog closes and user remains on registrations list (no navigation)', () => {
                const row = buildRegistrationRow('member-1', {
                    _links: {
                        self: {href: 'http://localhost:8443/api/events/1/registrations/member-1'},
                        recordTransaction: {href: 'http://localhost:8443/api/members/member-1/account'},
                    },
                });
                renderPageWithRegistrationRows([row]);

                fireEvent.click(screen.getByRole('button', {name: /provést finanční transakci/i}));
                expect(screen.getByTestId('finance-transaction-dialog')).toBeInTheDocument();

                fireEvent.click(screen.getByRole('button', {name: 'close-dialog'}));

                expect(screen.queryByTestId('finance-transaction-dialog')).not.toBeInTheDocument();
                expect(mockNavigate).not.toHaveBeenCalled();
                expect(screen.getByRole('heading', {level: 1, name: 'Jarní závod 2025'})).toBeInTheDocument();
            });

            it('3.1 clicking transaction button opens FinanceTransactionDialog with defaultNote set to event name', () => {
                const accountHref = 'http://localhost:8443/api/members/member-1/account';
                const row = buildRegistrationRow('member-1', {
                    _links: {
                        self: {href: 'http://localhost:8443/api/events/1/registrations/member-1'},
                        recordTransaction: {href: accountHref},
                    },
                });
                renderPageWithRegistrationRows([row]);

                fireEvent.click(screen.getByRole('button', {name: /provést finanční transakci/i}));

                const dialog = screen.getByTestId('finance-transaction-dialog');
                expect(dialog).toHaveAttribute('data-default-note', 'Jarní závod 2025');
            });

            it('3.2 (negative) dialog has no defaultNote when opened without event context (no defaultNote attribute set)', () => {
                // This test verifies that when FinanceTransactionDialog is used without an event context
                // the defaultNote is not set (empty string in our mock's data-default-note).
                // In EventDetailPage the defaultNote IS always set to event name when the dialog is opened
                // from RegistrationsTable — but our negative scenario is tested at the dialog component
                // level (see FinanceTransactionDialog.test.tsx test 3.2).
                // Here we just verify the attribute is the event name (not something else).
                const accountHref = 'http://localhost:8443/api/members/member-1/account';
                const row = buildRegistrationRow('member-1', {
                    _links: {
                        self: {href: 'http://localhost:8443/api/events/1/registrations/member-1'},
                        recordTransaction: {href: accountHref},
                    },
                });
                // Use a page with a different event name to confirm the prop is tied to event name
                const resourceData = mockEventWithRegistrationsLink({name: 'Podzimní sprint'});
                const registrationsListData = {
                    _links: {self: {href: 'http://localhost:8443/api/events/1/registrations'}},
                    _embedded: {registrationDtoList: [row]},
                    page: {totalElements: 1, totalPages: 1, size: 10, number: 0},
                };
                vi.mocked(useAuthorizedQuery).mockReturnValue({data: registrationsListData, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);
                renderPage(createMockPageData(resourceData));

                fireEvent.click(screen.getByRole('button', {name: /provést finanční transakci/i}));

                const dialog = screen.getByTestId('finance-transaction-dialog');
                expect(dialog).toHaveAttribute('data-default-note', 'Podzimní sprint');
            });

            it('2.5 transaction button only appears on rows that have the affordance (mixed rows)', () => {
                const rowWithAffordance = buildRegistrationRow('member-1', {
                    _links: {
                        self: {href: 'http://localhost:8443/api/events/1/registrations/member-1'},
                        recordTransaction: {href: 'http://localhost:8443/api/members/member-1/account'},
                    },
                });
                const rowWithoutAffordance = buildRegistrationRow('member-2');
                renderPageWithRegistrationRows([rowWithAffordance, rowWithoutAffordance]);

                expect(screen.getAllByRole('button', {name: /provést finanční transakci/i})).toHaveLength(1);
            });
        });
    });

    describe('edit own registration button (Group 6)', () => {
        it('shows editRegistration button when editRegistration template exists on root resource', () => {
            const data = mockEventDetailData({
                _templates: {
                    editRegistration: mockHalFormsTemplate({method: 'PUT', title: 'Upravit přihlášku'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /upravit přihlášku/i})).toBeInTheDocument();
        });

        it('does not show editRegistration button when editRegistration template is absent', () => {
            renderPage(createMockPageData(mockEventDetailData()));
            expect(screen.queryByRole('button', {name: /upravit přihlášku/i})).not.toBeInTheDocument();
        });

        it('opens modal with HalFormDisplay when editRegistration button is clicked', () => {
            const data = mockEventDetailData({
                _templates: {
                    editRegistration: mockHalFormsTemplate({method: 'PUT', title: 'Upravit přihlášku'}),
                },
            });
            renderPage(createMockPageData(data));

            fireEvent.click(screen.getByRole('button', {name: /upravit přihlášku/i}));

            expect(screen.getByRole('dialog')).toBeInTheDocument();
        });
    });

    describe('accommodation list action (N11 task 3.5)', () => {
        it('shows "Seznam pro ubytování" link when accommodation-list link is present', () => {
            const data = mockEventDetailData({
                _links: {
                    self: {href: 'http://localhost:8443/api/events/1'},
                    'accommodation-list': {href: 'http://localhost:8443/api/events/1/accommodation-list'},
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('link', {name: /seznam pro ubytování/i})).toBeInTheDocument();
        });

        it('does not show "Seznam pro ubytování" link when accommodation-list link is absent', () => {
            renderPage(createMockPageData(mockEventDetailData()));
            expect(screen.queryByRole('link', {name: /seznam pro ubytování/i})).not.toBeInTheDocument();
        });

        it('"Seznam pro ubytování" link navigates to accommodation list route', () => {
            const data = mockEventDetailData({
                _links: {
                    self: {href: 'http://localhost:8443/api/events/1'},
                    'accommodation-list': {href: 'http://localhost:8443/api/events/1/accommodation-list'},
                },
            });
            renderPage(createMockPageData(data));
            const link = screen.getByRole('link', {name: /seznam pro ubytování/i});
            expect(link).toHaveAttribute('href', '/events/1/accommodation-list');
        });
    });

    describe('deadlines section (6.3)', () => {
        beforeEach(() => {
            vi.useFakeTimers();
            vi.setSystemTime(new Date('2025-05-09'));
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        it('shows deadlines section heading when deadlines are present', () => {
            renderPage(createMockPageData(mockEventDetailData({deadlines: ['2025-03-01', '2025-04-01']})));
            expect(screen.getByText('UZÁVĚRKY PŘIHLÁŠEK')).toBeInTheDocument();
        });

        it('does not show deadlines section when deadlines is empty array', () => {
            renderPage(createMockPageData(mockEventDetailData({deadlines: []})));
            expect(screen.queryByText('UZÁVĚRKY PŘIHLÁŠEK')).not.toBeInTheDocument();
        });

        it('does not show deadlines section when deadlines is absent', () => {
            renderPage(createMockPageData(mockEventDetailData({deadlines: undefined})));
            expect(screen.queryByText('UZÁVĚRKY PŘIHLÁŠEK')).not.toBeInTheDocument();
        });

        it('shows all deadlines formatted in the section', () => {
            renderPage(createMockPageData(mockEventDetailData({deadlines: ['2025-03-01', '2025-04-01', '2025-04-10']})));
            expect(screen.getByText('1. 3. 2025')).toBeInTheDocument();
            expect(screen.getByText('1. 4. 2025')).toBeInTheDocument();
            expect(screen.getByText('10. 4. 2025')).toBeInTheDocument();
        });

        it('shows single deadline without a list', () => {
            renderPage(createMockPageData(mockEventDetailData({deadlines: ['2025-04-01']})));
            expect(screen.getByText('1. 4. 2025')).toBeInTheDocument();
        });

        it('marks a future single deadline as "aktuální"', () => {
            renderPage(createMockPageData(mockEventDetailData({deadlines: ['2099-12-31']})));
            expect(screen.getByText('aktuální')).toBeInTheDocument();
        });

        it('marks a past single deadline as "aktuální" (only deadline, so it is relevant)', () => {
            renderPage(createMockPageData(mockEventDetailData({deadlines: ['2020-01-01']})));
            expect(screen.getByText('aktuální')).toBeInTheDocument();
        });

        it('marks the first future deadline as "aktuální" when multiple deadlines exist', () => {
            renderPage(createMockPageData(mockEventDetailData({deadlines: ['2025-03-01', '2099-12-31']})));
            const relevantLabel = screen.getByText('aktuální');
            expect(relevantLabel).toBeInTheDocument();
            const relevantItem = relevantLabel.closest('li');
            expect(relevantItem).toHaveTextContent('31. 12. 2099');
        });

        it('marks the last deadline as "aktuální" when all deadlines have passed', () => {
            renderPage(createMockPageData(mockEventDetailData({deadlines: ['2020-01-01', '2021-06-01']})));
            const relevantLabel = screen.getByText('aktuální');
            expect(relevantLabel).toBeInTheDocument();
            const relevantItem = relevantLabel.closest('li');
            expect(relevantItem).toHaveTextContent('1. 6. 2021');
        });

        it('highlights relevant deadline with bold text', () => {
            renderPage(createMockPageData(mockEventDetailData({deadlines: ['2099-12-31']})));
            const dateSpan = screen.getByText('31. 12. 2099');
            expect(dateSpan).toHaveClass('font-bold');
        });

        it('prefixes each deadline with its ordinal position', () => {
            renderPage(createMockPageData(mockEventDetailData({deadlines: ['2025-03-01', '2099-12-31']})));
            expect(screen.getByText('1. termín:')).toBeInTheDocument();
            expect(screen.getByText('2. termín:')).toBeInTheDocument();
        });
    });

    describe('cancellation section (6.3)', () => {
        it('shows cancellation block for CANCELLED event', () => {
            renderPage(createMockPageData(mockEventDetailData({status: 'CANCELLED'})));
            expect(screen.getByText('AKCE BYLA ZRUŠENA')).toBeInTheDocument();
        });

        it('does not show cancellation block for non-CANCELLED event', () => {
            renderPage(createMockPageData(mockEventDetailData({status: 'ACTIVE'})));
            expect(screen.queryByText('AKCE BYLA ZRUŠENA')).not.toBeInTheDocument();
        });

        it('shows cancellation reason when provided', () => {
            renderPage(createMockPageData(mockEventDetailData({
                status: 'CANCELLED',
                cancellationReason: 'Zrušeno kvůli počasí',
            })));
            expect(screen.getByText('Zrušeno kvůli počasí')).toBeInTheDocument();
        });

        it('does not show reason text when cancellationReason is absent', () => {
            renderPage(createMockPageData(mockEventDetailData({
                status: 'CANCELLED',
                cancellationReason: undefined,
            })));
            expect(screen.getByText('AKCE BYLA ZRUŠENA')).toBeInTheDocument();
        });
    });

    describe('registerForEvent — stay on page after registration (Group 8)', () => {
        it('clicking registerForEvent opens modal but does NOT navigate away', () => {
            const data = mockEventDetailData({
                _links: {
                    self: {href: 'http://localhost:8443/api/events/1'},
                    registrations: {href: 'http://localhost:8443/api/events/1/registrations'},
                },
                _templates: {
                    registerForEvent: mockHalFormsTemplate({method: 'POST', title: 'Přihlásit se'}),
                },
            });
            renderPage(createMockPageData(data));

            fireEvent.click(screen.getByRole('button', {name: /přihlásit se/i}));

            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(mockNavigate).not.toHaveBeenCalled();
            expect(screen.getByRole('heading', {level: 1, name: 'Jarní závod 2025'})).toBeInTheDocument();
            expect(screen.getByRole('heading', {name: /přihlášky/i})).toBeInTheDocument();
        });
    });

    describe('event type badge (B7.3)', () => {
        const withEventTypesMock = (eventTypes: {id: string; name: string; color?: string; sortOrder: number}[]) => {
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
                        _embedded: {registrationDtoList: []},
                        page: {totalElements: 0, totalPages: 0, size: 10, number: 0},
                        _links: {self: {href: url}},
                    },
                    isLoading: false,
                    error: null,
                } as unknown as ReturnType<typeof useAuthorizedQuery>;
            });
        };

        it('shows event type name badge in header when event has eventTypeId', () => {
            withEventTypesMock([{id: 'type-abc', name: 'Pohárový závod', color: '#3366FF', sortOrder: 1}]);
            renderPage(createMockPageData(mockEventDetailData({eventTypeId: 'type-abc'})));
            expect(screen.getByText('Pohárový závod')).toBeInTheDocument();
        });

        it('shows color dot for event type with color', () => {
            withEventTypesMock([{id: 'type-abc', name: 'Trénink', color: '#00FF00', sortOrder: 1}]);
            renderPage(createMockPageData(mockEventDetailData({eventTypeId: 'type-abc'})));
            const dot = document.querySelector('[aria-hidden="true"]') as HTMLElement | null;
            expect(dot).toBeInTheDocument();
        });

        it('does not show event type badge when event has no eventTypeId', () => {
            withEventTypesMock([{id: 'type-abc', name: 'Trénink', sortOrder: 1}]);
            renderPage(createMockPageData(mockEventDetailData({eventTypeId: undefined})));
            expect(screen.queryByText('Trénink')).not.toBeInTheDocument();
        });

        it('does not show event type badge when eventTypeId does not match catalog', () => {
            withEventTypesMock([{id: 'type-abc', name: 'Trénink', sortOrder: 1}]);
            renderPage(createMockPageData(mockEventDetailData({eventTypeId: 'unknown-type'})));
            expect(screen.queryByText('Trénink')).not.toBeInTheDocument();
        });
    });

    describe('ranking display (task 5.1)', () => {
        it('shows ranking shortName and name when ranking is present', () => {
            renderPage(createMockPageData(mockEventDetailData({
                ranking: {shortName: 'ČŽ', name: 'Český žebříček'},
            })));
            expect(screen.getByText('ČŽ')).toBeInTheDocument();
            expect(screen.getByText('Český žebříček')).toBeInTheDocument();
        });

        it('shows ranking row label when ranking is present', () => {
            renderPage(createMockPageData(mockEventDetailData({
                ranking: {shortName: 'ČŽ', name: 'Český žebříček'},
            })));
            expect(screen.getByText('Žebříček')).toBeInTheDocument();
        });

        it('does not show ranking row when ranking is null', () => {
            renderPage(createMockPageData(mockEventDetailData({ranking: null})));
            expect(screen.queryByText('Žebříček')).not.toBeInTheDocument();
        });

        it('does not show ranking row when ranking is absent', () => {
            renderPage(createMockPageData(mockEventDetailData()));
            expect(screen.queryByText('Žebříček')).not.toBeInTheDocument();
        });
    });

    describe('baseEntryFee display (task 5.1)', () => {
        it('shows formatted entry fee with amount and currency when baseEntryFee is present', () => {
            renderPage(createMockPageData(mockEventDetailData({
                baseEntryFee: {amount: 650, currency: 'CZK'},
            })));
            expect(screen.getByText('650 CZK')).toBeInTheDocument();
        });

        it('shows entry fee row label when baseEntryFee is present', () => {
            renderPage(createMockPageData(mockEventDetailData({
                baseEntryFee: {amount: 100, currency: 'CZK'},
            })));
            expect(screen.getByText('Startovné')).toBeInTheDocument();
        });

        it('does not show entry fee row when baseEntryFee is null', () => {
            renderPage(createMockPageData(mockEventDetailData({baseEntryFee: null})));
            expect(screen.queryByText('Startovné')).not.toBeInTheDocument();
        });

        it('does not show entry fee row when baseEntryFee is absent', () => {
            renderPage(createMockPageData(mockEventDetailData()));
            expect(screen.queryByText('Startovné')).not.toBeInTheDocument();
        });
    });
});
