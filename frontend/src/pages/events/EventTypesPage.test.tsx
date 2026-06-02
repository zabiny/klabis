import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import type {UseHalPageDataReturn} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {EventTypesPage} from './EventTypesPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {HalFormDisplay, type HalFormDisplayProps} from '../../components/HalNavigator2/HalFormDisplay';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../components/HalNavigator2/HalFormDisplay.tsx', () => ({
    HalFormDisplay: vi.fn(() => <div data-testid="hal-form-display"/>),
}));

vi.mock('../../components/UI', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../components/UI')>();
    return {
        ...actual,
        Modal: ({isOpen, children, title}: {isOpen: boolean; children: React.ReactNode; title: string}) =>
            isOpen ? <div data-testid="modal-overlay" data-title={title}>{children}</div> : null,
    };
});

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
        pathname: '/event-types',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/event-types'}),
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
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/event-types']}>
                <EventTypesPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
};

const buildEventTypeRow = (overrides?: Record<string, unknown>) => ({
    id: 'type-1',
    name: 'Trénink',
    color: '#FF5733',
    sortOrder: 1,
    _links: {self: {href: '/api/event-types/type-1'}},
    ...overrides,
});

const renderPageWithEventTypes = (eventTypes: unknown[]) => {
    const resourceData: HalResponse = {
        _links: {self: {href: 'http://localhost/api/event-types'}},
        _embedded: {eventTypeDtoList: eventTypes},
        page: {size: 10, totalElements: eventTypes.length, totalPages: 1, number: 0},
    };
    vi.mocked(useAuthorizedQuery).mockReturnValue({data: resourceData, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);
    const pageData = createMockPageData(resourceData);
    return renderPage(pageData);
};

describe('EventTypesPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders page title "Typy akcí"', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByRole('heading', {name: 'Typy akcí'})).toBeInTheDocument();
    });

    describe('"Přidat typ akce" button', () => {
        it('shows button when createEventType template exists', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/event-types'}},
                _templates: {
                    createEventType: mockHalFormsTemplate({
                        method: 'POST',
                        target: '/api/event-types',
                        title: 'Přidat typ akce',
                    }),
                },
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.getByRole('button', {name: /přidat typ akce/i})).toBeInTheDocument();
        });

        it('does NOT show button when createEventType template is absent', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/event-types'}},
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.queryByRole('button', {name: /přidat typ akce/i})).not.toBeInTheDocument();
        });
    });

    describe('table renders event type rows', () => {
        it('renders event type name in table', () => {
            renderPageWithEventTypes([buildEventTypeRow()]);
            expect(screen.getByText('Trénink')).toBeInTheDocument();
        });

        it('renders sortOrder value in table', () => {
            renderPageWithEventTypes([buildEventTypeRow({sortOrder: 42})]);
            expect(screen.getByText('42')).toBeInTheDocument();
        });

        it('renders color swatch for event type with color', () => {
            renderPageWithEventTypes([buildEventTypeRow({color: '#FF5733'})]);
            const swatch = screen.getByTitle('#FF5733');
            expect(swatch).toBeInTheDocument();
        });
    });

    describe('Akce column — action buttons on event type rows', () => {
        it('renders Akce column header', () => {
            renderPage(createMockPageData({_links: {self: {href: '/api/event-types'}}}));
            expect(screen.getByText('Akce')).toBeInTheDocument();
        });

        it('shows edit button when _templates.updateEventType is present on a row', () => {
            const eventType = buildEventTypeRow({
                _templates: {updateEventType: mockHalFormsTemplate({method: 'PUT', title: 'Upravit typ akce'})},
            });
            renderPageWithEventTypes([eventType]);
            expect(screen.getByRole('button', {name: 'Upravit'})).toBeInTheDocument();
        });

        it('does NOT show edit button when _templates.updateEventType is absent', () => {
            renderPageWithEventTypes([buildEventTypeRow()]);
            expect(screen.queryByRole('button', {name: 'Upravit'})).not.toBeInTheDocument();
        });

        it('shows delete button when _templates.deleteEventType is present on a row', () => {
            const eventType = buildEventTypeRow({
                _templates: {deleteEventType: mockHalFormsTemplate({method: 'DELETE', title: 'Smazat typ akce'})},
            });
            renderPageWithEventTypes([eventType]);
            expect(screen.getByRole('button', {name: 'Smazat typ akce'})).toBeInTheDocument();
        });

        it('does NOT show delete button when _templates.deleteEventType is absent', () => {
            renderPageWithEventTypes([buildEventTypeRow()]);
            expect(screen.queryByRole('button', {name: 'Smazat typ akce'})).not.toBeInTheDocument();
        });

        it('opens modal with HalFormDisplay when edit button is clicked', () => {
            const eventType = buildEventTypeRow({
                _templates: {updateEventType: mockHalFormsTemplate({method: 'PUT', title: 'Upravit typ akce'})},
            });
            renderPageWithEventTypes([eventType]);

            fireEvent.click(screen.getByRole('button', {name: 'Upravit'}));

            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
            expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
        });

        it('opens modal with HalFormDisplay when delete button is clicked', () => {
            const eventType = buildEventTypeRow({
                _templates: {deleteEventType: mockHalFormsTemplate({method: 'DELETE', title: 'Smazat typ akce'})},
            });
            renderPageWithEventTypes([eventType]);

            fireEvent.click(screen.getByRole('button', {name: 'Smazat typ akce'}));

            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
            expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
        });

        it('passes item self link as resourceUrl to HalFormDisplay when edit button is clicked', () => {
            const capturedProps: HalFormDisplayProps[] = [];
            vi.mocked(HalFormDisplay).mockImplementation((props) => {
                capturedProps.push(props);
                return <div data-testid="hal-form-display"/>;
            });

            const eventType = buildEventTypeRow({
                _links: {self: {href: 'https://localhost:8443/api/event-types/type-1'}},
                _templates: {updateEventType: mockHalFormsTemplate({method: 'PUT', title: 'Upravit typ akce'})},
            });
            renderPageWithEventTypes([eventType]);

            fireEvent.click(screen.getByRole('button', {name: 'Upravit'}));

            expect(capturedProps[capturedProps.length - 1].resourceUrl).toBe('https://localhost:8443/api/event-types/type-1');
        });
    });

    describe('row click behaviour', () => {
        it('does not call navigateToResource when a row is clicked', () => {
            const navigateToResource = vi.fn();
            const resourceData: HalResponse = {
                _links: {self: {href: 'http://localhost/api/event-types'}},
                _embedded: {eventTypeDtoList: [buildEventTypeRow()]},
                page: {size: 10, totalElements: 1, totalPages: 1, number: 0},
            };
            vi.mocked(useAuthorizedQuery).mockReturnValue({data: resourceData, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);
            renderPage(createMockPageData(resourceData, {route: {pathname: '/event-types', navigateToResource, refetch: async () => {}, queryState: 'success' as const, getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/event-types'})}}));

            fireEvent.click(screen.getByText('Trénink'));

            expect(navigateToResource).not.toHaveBeenCalled();
        });
    });
});
