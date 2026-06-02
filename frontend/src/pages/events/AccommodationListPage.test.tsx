import '@testing-library/jest-dom';
import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {AccommodationListPage} from './AccommodationListPage';
import {vi} from 'vitest';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {authorizedFetch} from '../../api/authorizedFetch';

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useParams: () => ({id: '42'}),
    };
});

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

vi.mock('../ErrorPage', () => ({
    ErrorPage: ({error}: {error?: {responseStatus?: number; message?: string} | null}) => (
        <div data-testid="error-page" data-status={error?.responseStatus}>
            {error?.responseStatus === 403
                ? <div data-testid="forbidden-page">Přístup odepřen</div>
                : <div>{error?.message}</div>
            }
        </div>
    ),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

vi.mock('../../api/authorizedFetch', async () => {
    const actual = await vi.importActual('../../api/authorizedFetch');
    return {
        ...actual,
        authorizedFetch: vi.fn(),
    };
});

const EVENT_URL = '/api/events/42';
const ACCOMMODATION_LIST_URL = '/api/events/42/accommodation-list';

const buildAccommodationItem = (overrides?: Record<string, unknown>) => ({
    firstName: 'Jana',
    lastName: 'Nováková',
    identityCardNumber: 'AB123456',
    identityCardValidityDate: '2027-12-31',
    dateOfBirth: '1990-05-15',
    addressStreet: 'Hlavní 1',
    addressCity: 'Brno',
    addressPostalCode: '60200',
    addressCountry: 'CZ',
    ...overrides,
});

const buildEventData = (overrides?: Record<string, unknown>) => ({
    name: 'Jarní závod 2025',
    _links: {
        self: {href: EVENT_URL},
        'accommodation-list': {href: ACCOMMODATION_LIST_URL},
        ...((overrides?._links as object) ?? {}),
    },
    ...overrides,
});

const buildListData = (items: unknown[]) => ({
    _embedded: {
        accommodationList: items,
    },
    _links: {self: {href: ACCOMMODATION_LIST_URL}},
});

const setupQueryMocks = (eventData: unknown, listData: unknown) => {
    vi.mocked(useAuthorizedQuery).mockImplementation((_url: string) => {
        if (_url.includes('/api/events/42/accommodation-list')) {
            return {data: listData, isLoading: false, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>;
        }
        return {data: eventData, isLoading: false, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>;
    });
};

const renderPage = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/events/42/accommodation-list']}>
                <AccommodationListPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('AccommodationListPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('loading and error states', () => {
        it('shows loading indicator while fetching event data', () => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: undefined,
                isLoading: true,
                error: null,
            } as unknown as ReturnType<typeof useAuthorizedQuery>);
            renderPage();
            expect(screen.queryByRole('table')).not.toBeInTheDocument();
        });

        it('shows error message when event fetch fails', () => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: undefined,
                isLoading: false,
                error: new Error('Fetch failed'),
            } as unknown as ReturnType<typeof useAuthorizedQuery>);
            renderPage();
            expect(screen.getByText(/fetch failed/i)).toBeInTheDocument();
        });

        it('shows ForbiddenPage when event fetch returns 403', () => {
            const error = Object.assign(new Error('HTTP 403: Forbidden'), {responseStatus: 403});
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: undefined,
                isLoading: false,
                error,
            } as unknown as ReturnType<typeof useAuthorizedQuery>);
            renderPage();
            expect(screen.getByTestId('forbidden-page')).toBeInTheDocument();
        });
    });

    describe('page structure', () => {
        it('renders page heading', () => {
            setupQueryMocks(buildEventData(), buildListData([]));
            renderPage();
            expect(screen.getByRole('heading', {name: /seznam pro ubytování/i})).toBeInTheDocument();
        });

        it('renders event name as subtitle', () => {
            setupQueryMocks(buildEventData(), buildListData([]));
            renderPage();
            expect(screen.getByText('Jarní závod 2025')).toBeInTheDocument();
        });

        it('renders back link to event detail page', () => {
            setupQueryMocks(buildEventData(), buildListData([]));
            renderPage();
            const backLink = screen.getByRole('link', {name: /zpět/i});
            expect(backLink).toHaveAttribute('href', '/events/42');
        });

        it('renders Tisknout button', () => {
            setupQueryMocks(buildEventData(), buildListData([]));
            renderPage();
            expect(screen.getByRole('button', {name: /tisknout/i})).toBeInTheDocument();
        });
    });

    describe('table columns', () => {
        it('renders all required column headers', () => {
            setupQueryMocks(buildEventData(), buildListData([]));
            renderPage();
            expect(screen.getByRole('columnheader', {name: /jméno/i})).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: /příjmení/i})).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: /číslo op/i})).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: /platnost op/i})).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: /datum narození/i})).toBeInTheDocument();
            expect(screen.getByRole('columnheader', {name: /adresa/i})).toBeInTheDocument();
        });
    });

    describe('table rows with full data', () => {
        it('renders member first name and last name', () => {
            setupQueryMocks(buildEventData(), buildListData([buildAccommodationItem()]));
            renderPage();
            expect(screen.getByRole('cell', {name: 'Jana'})).toBeInTheDocument();
            expect(screen.getByRole('cell', {name: 'Nováková'})).toBeInTheDocument();
        });

        it('renders identity card number', () => {
            setupQueryMocks(buildEventData(), buildListData([buildAccommodationItem()]));
            renderPage();
            expect(screen.getByRole('cell', {name: 'AB123456'})).toBeInTheDocument();
        });

        it('renders identity card validity date formatted as Czech date', () => {
            setupQueryMocks(buildEventData(), buildListData([buildAccommodationItem()]));
            renderPage();
            expect(screen.getByRole('cell', {name: '31. 12. 2027'})).toBeInTheDocument();
        });

        it('renders date of birth formatted as Czech date', () => {
            setupQueryMocks(buildEventData(), buildListData([buildAccommodationItem()]));
            renderPage();
            expect(screen.getByRole('cell', {name: '15. 5. 1990'})).toBeInTheDocument();
        });

        it('renders address from flat fields', () => {
            setupQueryMocks(buildEventData(), buildListData([buildAccommodationItem()]));
            renderPage();
            expect(screen.getByRole('cell', {name: /hlavní 1.*brno/i})).toBeInTheDocument();
        });
    });

    describe('"neuvedeno" fallback for missing fields', () => {
        it('renders "neuvedeno" for missing identityCardNumber', () => {
            const item = buildAccommodationItem({identityCardNumber: undefined, identityCardValidityDate: undefined});
            setupQueryMocks(buildEventData(), buildListData([item]));
            renderPage();
            const neuvedenoCells = screen.getAllByText('neuvedeno');
            expect(neuvedenoCells.length).toBeGreaterThanOrEqual(2);
        });

        it('renders "neuvedeno" for missing dateOfBirth', () => {
            const item = buildAccommodationItem({dateOfBirth: undefined});
            setupQueryMocks(buildEventData(), buildListData([item]));
            renderPage();
            expect(screen.getByText('neuvedeno')).toBeInTheDocument();
        });

        it('renders "neuvedeno" for missing address fields', () => {
            const item = buildAccommodationItem({
                addressStreet: undefined,
                addressCity: undefined,
                addressPostalCode: undefined,
                addressCountry: undefined,
            });
            setupQueryMocks(buildEventData(), buildListData([item]));
            renderPage();
            expect(screen.getByText('neuvedeno')).toBeInTheDocument();
        });
    });

    describe('multiple rows', () => {
        it('renders all rows from accommodation list', () => {
            const items = [
                buildAccommodationItem({firstName: 'Jana', lastName: 'Nováková'}),
                buildAccommodationItem({firstName: 'Petr', lastName: 'Novák'}),
            ];
            setupQueryMocks(buildEventData(), buildListData(items));
            renderPage();
            expect(screen.getByRole('cell', {name: 'Jana'})).toBeInTheDocument();
            expect(screen.getByRole('cell', {name: 'Petr'})).toBeInTheDocument();
        });

        it('renders empty table when no members', () => {
            setupQueryMocks(buildEventData(), buildListData([]));
            renderPage();
            expect(screen.getByRole('table')).toBeInTheDocument();
            expect(screen.queryByRole('cell', {name: /jana/i})).not.toBeInTheDocument();
        });
    });

    describe('Stáhnout CSV button', () => {
        const mockAuthorizedFetch = authorizedFetch as ReturnType<typeof vi.fn>;

        beforeEach(() => {
            setupQueryMocks(buildEventData(), buildListData([]));
            vi.stubGlobal('URL', {
                createObjectURL: vi.fn().mockReturnValue('blob:mock-url'),
                revokeObjectURL: vi.fn(),
            });
        });

        afterEach(() => {
            vi.unstubAllGlobals();
        });

        it('renders "Stáhnout CSV" button', () => {
            renderPage();
            expect(screen.getByRole('button', {name: /stáhnout csv/i})).toBeInTheDocument();
        });

        it('disables "Stáhnout CSV" button while download is in flight', async () => {
            let resolveDownload!: (value: Response) => void;
            const pendingDownload = new Promise<Response>(resolve => { resolveDownload = resolve; });
            mockAuthorizedFetch.mockReturnValue(pendingDownload);

            renderPage();
            const button = screen.getByRole('button', {name: /stáhnout csv/i});
            fireEvent.click(button);

            await waitFor(() => {
                expect(button).toBeDisabled();
            });

            resolveDownload(new Response('csv-data', {
                status: 200,
                headers: {'Content-Disposition': 'attachment; filename="ubytovani-test.csv"', 'Content-Type': 'text/csv'},
            }));
        });

        const captureAppendedAnchor = () => {
            const appendedAnchors: HTMLAnchorElement[] = [];
            const originalAppendChild = document.body.appendChild.bind(document.body);
            vi.spyOn(document.body, 'appendChild').mockImplementation((node) => {
                if (node instanceof HTMLAnchorElement) {
                    vi.spyOn(node, 'click');
                    appendedAnchors.push(node);
                }
                return originalAppendChild(node);
            });
            vi.spyOn(document.body, 'removeChild').mockImplementation((node) => node as Node);
            return appendedAnchors;
        };

        it('calls authorizedFetch with Accept: text/csv header on click', async () => {
            mockAuthorizedFetch.mockResolvedValue(new Response('csv', {
                status: 200,
                headers: {'Content-Disposition': 'attachment; filename="ubytovani-test.csv"'},
            }));

            renderPage();
            fireEvent.click(screen.getByRole('button', {name: /stáhnout csv/i}));

            await waitFor(() => {
                expect(mockAuthorizedFetch).toHaveBeenCalledWith(
                    ACCOMMODATION_LIST_URL,
                    expect.objectContaining({
                        headers: expect.objectContaining({'Accept': 'text/csv'}),
                    }),
                    true
                );
            });
        });

        it('derives filename from Content-Disposition header', async () => {
            mockAuthorizedFetch.mockResolvedValue(new Response('csv', {
                status: 200,
                headers: {'Content-Disposition': 'attachment; filename="ubytovani-zimni-soustredeni-2026.csv"'},
            }));

            renderPage();
            const anchors = captureAppendedAnchor();
            fireEvent.click(screen.getByRole('button', {name: /stáhnout csv/i}));

            await waitFor(() => {
                expect(anchors).toHaveLength(1);
                expect(anchors[0].download).toBe('ubytovani-zimni-soustredeni-2026.csv');
            });
        });

        it('falls back to ubytovani.csv when Content-Disposition header is absent', async () => {
            mockAuthorizedFetch.mockResolvedValue(new Response('csv', {status: 200}));

            renderPage();
            const anchors = captureAppendedAnchor();
            fireEvent.click(screen.getByRole('button', {name: /stáhnout csv/i}));

            await waitFor(() => {
                expect(anchors).toHaveLength(1);
                expect(anchors[0].download).toBe('ubytovani.csv');
            });
        });

        it('revokes object URL after download is triggered', async () => {
            mockAuthorizedFetch.mockResolvedValue(new Response('csv', {
                status: 200,
                headers: {'Content-Disposition': 'attachment; filename="ubytovani-test.csv"'},
            }));

            renderPage();
            captureAppendedAnchor();
            fireEvent.click(screen.getByRole('button', {name: /stáhnout csv/i}));

            await waitFor(() => {
                expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
            });
        });

        it('re-enables button after successful download', async () => {
            mockAuthorizedFetch.mockResolvedValue(new Response('csv', {
                status: 200,
                headers: {'Content-Disposition': 'attachment; filename="ubytovani-test.csv"'},
            }));

            renderPage();
            captureAppendedAnchor();
            const button = screen.getByRole('button', {name: /stáhnout csv/i});
            fireEvent.click(button);

            await waitFor(() => {
                expect(button).not.toBeDisabled();
            });
        });
    });
});
