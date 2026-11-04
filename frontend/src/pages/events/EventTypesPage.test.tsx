import '@testing-library/jest-dom';
import {render, screen, fireEvent, within, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import type {UseHalPageDataReturn} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {EventTypesPage} from './EventTypesPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {ToastProvider} from '../../contexts/ToastContext.tsx';

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

vi.mock('../../hooks/useHalFormData', () => ({
    useHalFormData: (_template: unknown, resourceData: Record<string, unknown>) => ({
        formData: resourceData ?? {},
        isLoadingTargetData: false,
        targetFetchError: null,
        refetchTargetData: vi.fn(),
    }),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
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
    navigateToResource: vi.fn(),
    refetch: async () => {},
    getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/event-types'}),
    ...overrides,
});

const renderPage = (pageData: UseHalPageDataReturn) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/event-types']}>
                <ToastProvider>
                <EventTypesPage />
                </ToastProvider>
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

        it('opens dialog titled by the template when edit button is clicked', () => {
            const row = buildEventTypeRow({
                _templates: {updateEventType: mockHalFormsTemplate({method: 'PUT', title: 'Upravit typ akce'})},
            });
            renderPageWithEventTypes([row]);

            fireEvent.click(screen.getByRole('button', {name: 'Upravit'}));

            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(within(screen.getByRole('dialog')).getAllByRole('heading', {name: 'Upravit typ akce'})[0]).toBeInTheDocument();
        });

        it('opens dialog titled by the template when delete button is clicked', () => {
            const row = buildEventTypeRow({
                _templates: {deleteEventType: mockHalFormsTemplate({method: 'DELETE', title: 'Smazat typ akce'})},
            });
            renderPageWithEventTypes([row]);

            fireEvent.click(screen.getByRole('button', {name: 'Smazat typ akce'}));

            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(within(screen.getByRole('dialog')).getAllByRole('heading', {name: 'Smazat typ akce'})[0]).toBeInTheDocument();
        });

        it('submits to the item self link path when the template has no target', () => {
            const mutate = vi.fn();
            vi.mocked(useAuthorizedMutation).mockReturnValue({mutate, isPending: false, error: null} as unknown as ReturnType<typeof useAuthorizedMutation>);
            const row = buildEventTypeRow({
                _links: {self: {href: 'https://localhost:8443/api/event-types/type-1'}},
                _templates: {updateEventType: mockHalFormsTemplate({method: 'PUT', title: 'Upravit typ akce', target: undefined, properties: []})},
            });
            renderPageWithEventTypes([row]);

            fireEvent.click(screen.getByRole('button', {name: 'Upravit'}));
            fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', {name: /uložit|odeslat|potvrdit/i}));

            return waitFor(() => expect(mutate).toHaveBeenCalledWith(expect.objectContaining({url: expect.stringMatching(/\/api\/event-types\/type-1$/)}), expect.anything()));
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
            renderPage(createMockPageData(resourceData, {navigateToResource, refetch: async () => {}, getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/event-types'})}));

            fireEvent.click(screen.getByText('Trénink'));

            expect(navigateToResource).not.toHaveBeenCalled();
        });
    });
});
