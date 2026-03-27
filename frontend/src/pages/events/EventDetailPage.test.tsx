import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormProvider} from '../../contexts/HalFormContext';
import {HalFormsPageLayout} from '../../components/HalNavigator2/HalFormsPageLayout';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {EventDetailPage} from './EventDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

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
}));

vi.mock('../../components/UI/ModalOverlay.tsx', () => ({
    ModalOverlay: ({isOpen, children, onClose, title}: any) => (
        isOpen ? (
            <div data-testid="modal-overlay" role="dialog">
                {title && <h4>{title}</h4>}
                {children}
                <button onClick={onClose}>Close</button>
            </div>
        ) : null
    ),
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
        expect(screen.getByText('ACTIVE')).toBeInTheDocument();
    });

    it('renders coordinator id when present', () => {
        renderPage(createMockPageData(mockEventDetailData()));
        expect(screen.getByText('42')).toBeInTheDocument();
    });

    it('does not render coordinator when absent', () => {
        renderPage(createMockPageData(mockEventDetailData({eventCoordinatorId: undefined})));
        expect(screen.queryByText(/koordinátor/i)).not.toBeInTheDocument();
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
                    cancelEvent: mockHalFormsTemplate({title: 'Zrušit závod'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /zrušit závod/i})).toBeInTheDocument();
        });

        it('shows finishEvent button when template exists', () => {
            const data = mockEventDetailData({
                _templates: {
                    finishEvent: mockHalFormsTemplate({title: 'Dokončit'}),
                },
            });
            renderPage(createMockPageData(data));
            expect(screen.getByRole('button', {name: /dokončit/i})).toBeInTheDocument();
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

    describe('registrations section', () => {
        it('shows registrations section heading', () => {
            renderPage(createMockPageData(mockEventDetailData()));
            expect(screen.getByRole('heading', {name: /přihlášky/i})).toBeInTheDocument();
        });

        it('shows registrations table with correct columns', () => {
            renderPage(createMockPageData(mockEventDetailData()));
            expect(screen.getByRole('table')).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: 'Jméno'})).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: 'Příjmení'})).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: 'Datum přihlášení'})).toBeInTheDocument();
        });
    });
});
