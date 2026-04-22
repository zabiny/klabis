import '@testing-library/jest-dom';
import {fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormProvider} from '../../contexts/HalFormContext';
import {HalFormsPageLayout} from '../../components/HalNavigator2/HalFormsPageLayout';
import {useHalPageData} from '../../hooks/useHalPageData';
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
    toHref: vi.fn((source: any) => {
        if (Array.isArray(source)) return source[0]?.href ?? '';
        return source?.href ?? '';
    }),
}));

vi.mock('../../components/UI/Modal.tsx', () => ({
    Modal: ({isOpen, children, onClose, title}: any) => (
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
    HalSubresourceProvider: ({subresourceLinkName, children}: any) => (
        <div data-testid={`subresource-${subresourceLinkName}`}>{children}</div>
    ),
    useHalRoute: vi.fn(() => ({
        resourceData: {firstName: 'Jan', lastName: 'Novák', _links: {self: {href: '/api/members/42'}}},
        navigateToResource: vi.fn(),
        isLoading: false,
        error: null,
    })),
}));


const mockEventDetailData = (overrides?: Partial<any>): HalResponse => ({
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

const createMockPageData = (resourceData: HalResponse | null, overrides?: any) => ({
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

const renderPage = (pageData: any) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0, staleTime: Infinity}}});
    (globalThis as any).fetch = vi.fn().mockResolvedValue({
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
                {name: 'eventCoordinatorId', prompt: 'Koordinátor', type: 'text', value: '42'},
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
        const mockEventWithRegistrationsLink = (overrides?: Partial<any>) => mockEventDetailData({
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

        it('shows registrations table with correct columns when registrations link is present', () => {
            renderPage(createMockPageData(mockEventWithRegistrationsLink()));
            expect(screen.getByRole('table')).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: 'Jméno'})).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: 'Příjmení'})).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: 'Datum přihlášení'})).toBeInTheDocument();
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

        describe('edit button in registrations list row (Group 7)', () => {
            const buildRegistrationRow = (overrides?: Record<string, unknown>) => ({
                firstName: 'Jana',
                lastName: 'Nováková',
                registeredAt: '2025-03-10T10:00:00',
                _links: {self: {href: 'http://localhost:8443/api/events/1/registrations/member-1'}},
                ...overrides,
            });

            const renderPageWithRegistrationRows = (rows: unknown[], eventOverrides?: Partial<any>) => {
                const resourceData = mockEventWithRegistrationsLink(eventOverrides);
                const registrationsListData = {
                    _links: {self: {href: 'http://localhost:8443/api/events/1/registrations'}},
                    _embedded: {registrationDtoList: rows},
                    page: {totalElements: rows.length, totalPages: 1, size: 10, number: 0},
                };
                vi.mocked(useAuthorizedQuery)
                    .mockReturnValue({data: registrationsListData, error: null} as any);
                return renderPage(createMockPageData(resourceData));
            };

            it('shows Akce column header in registrations table when registrations link is present', () => {
                renderPageWithRegistrationRows([buildRegistrationRow()]);
                expect(screen.getByRole('columnheader', {name: /akce/i})).toBeInTheDocument();
            });

            it('shows edit button on row that has _templates.editRegistration', () => {
                const row = buildRegistrationRow({
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
                const row = buildRegistrationRow({
                    _templates: {editRegistration: mockHalFormsTemplate({method: 'PUT', title: 'Upravit přihlášku'})},
                });
                renderPageWithRegistrationRows([row]);

                fireEvent.click(screen.getByRole('button', {name: /upravit přihlášku/i}));

                expect(screen.getByRole('dialog')).toBeInTheDocument();
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            });
        });

        describe('edit button for EVENTS:REGISTRATIONS holder (Group 9)', () => {
            const buildRegistrationRow = (memberId: string, overrides?: Record<string, unknown>) => ({
                firstName: 'Jana',
                lastName: 'Nováková',
                registeredAt: '2025-03-10T10:00:00',
                _links: {self: {href: `http://localhost:8443/api/events/1/registrations/${memberId}`}},
                ...overrides,
            });

            const renderPageWithRegistrationRows = (rows: unknown[], eventOverrides?: Partial<any>) => {
                const resourceData = mockEventWithRegistrationsLink(eventOverrides);
                const registrationsListData = {
                    _links: {self: {href: 'http://localhost:8443/api/events/1/registrations'}},
                    _embedded: {registrationDtoList: rows},
                    page: {totalElements: rows.length, totalPages: 1, size: 10, number: 0},
                };
                vi.mocked(useAuthorizedQuery)
                    .mockReturnValue({data: registrationsListData, error: null} as any);
                return renderPage(createMockPageData(resourceData));
            };

            it('shows edit button on every row when all rows have _templates.editRegistration (EVENTS:REGISTRATIONS holder)', () => {
                // The backend sends editRegistration template on every row for holders of EVENTS:REGISTRATIONS.
                // The frontend must render the button purely from the template — no actingMember equality guard.
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

    describe('registerForEvent — stay on page after registration (Group 8)', () => {
        it('clicking registerForEvent opens modal but does NOT navigate away', () => {
            const data = mockEventDetailData({
                _templates: {
                    registerForEvent: mockHalFormsTemplate({method: 'POST', title: 'Přihlásit se'}),
                },
            });
            renderPage(createMockPageData(data));

            fireEvent.click(screen.getByRole('button', {name: /přihlásit se/i}));

            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(mockNavigate).not.toHaveBeenCalled();
            expect(screen.getByRole('heading', {level: 1, name: 'Jarní závod 2025'})).toBeInTheDocument();
        });

        it('registerForEvent button passes navigateOnSuccess=false so Location header is ignored', () => {
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
});
