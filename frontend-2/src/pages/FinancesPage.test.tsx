import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import {MemberFinancePage, MemberName} from './FinancesPage';
import {mockFinanceResource, mockMemberResource} from '../__mocks__/halData';
import {createMockResponse} from '../__mocks__/mockFetch';
import {HalRouteProvider} from '../contexts/HalRouteContext';
import {type Mock, vi} from 'vitest';
import {HalFormProvider} from "../contexts/HalFormContext.tsx";

// Mock the auth user manager to return a user with access token
vi.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockResolvedValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

describe('MemberFinancePage', () => {
    let queryClient: QueryClient;
    let fetchSpy: Mock;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        vi.clearAllMocks();
        // Mock global fetch
        fetchSpy = vi.fn() as Mock;
        (globalThis as any).fetch = fetchSpy;
    });

    afterEach(() => {
        delete (globalThis as any).fetch;
    });

    // For integration tests with real HalRouteProvider and mocked API
    const renderPage = (ui: React.ReactElement, initialRoute = '/finances/123') => {
        return render(
            <QueryClientProvider client={queryClient}>
                <HalFormProvider>
                    <MemoryRouter initialEntries={[initialRoute]}>
                        <HalRouteProvider>
                            {ui}
                        </HalRouteProvider>
                    </MemoryRouter>
                </HalFormProvider>
            </QueryClientProvider>
        );
    };

    const mockTransactionData = {
        _embedded: {
            transactionItemResponseList: [
                {
                    id: 1,
                    date: '2025-01-15',
                    amount: 500,
                    note: 'Monthly deposit',
                    type: 'deposit',
                    _links: {
                        self: {href: 'https://test.com/api/transactions/1'},
                    },
                },
                {
                    id: 2,
                    date: '2025-01-10',
                    amount: 100,
                    note: 'Withdrawal',
                    type: 'withdraw',
                    _links: {
                        self: {href: 'https://test.com/api/transactions/2'},
                    },
                },
            ],
        },
        page: {
            totalElements: 2,
            totalPages: 1,
            size: 10,
            number: 0,
        },
        _links: {
            self: {href: 'https://test.com/api/finances/123/transactions'},
        },
    };

    const mockFinanceData = mockFinanceResource({
        balance: 1500,
        _links: {
            self: {href: 'https://test.com/api/finances/123'},
            owner: {href: 'https://test.com/api/members/456'},
            transactions: {href: 'https://test.com/api/finances/123/transactions'},
        },
        _templates: {
            deposit: {
                title: 'Deposit',
                method: 'POST',
                contentType: 'application/x-www-form-urlencoded',
                properties: [
                    {
                        name: 'amount',
                        prompt: 'Amount',
                        required: true,
                        type: 'number',
                    },
                ],
                target: 'https://test.com/api/finances/123/deposit',
            },
        },
    });

    describe('Data Loading and Display', () => {
        beforeEach(() => {
            // Mock implementation to handle multiple endpoints and query parameters
            fetchSpy.mockImplementation((url: string) => {
                // Remove query parameters for endpoint matching
                const baseUrl = url.split('?')[0];

                if (baseUrl.endsWith('/transactions') || baseUrl.includes('/transactions/')) {
                    return Promise.resolve(createMockResponse(mockTransactionData));
                }

                // Default response for finance data
                return Promise.resolve(createMockResponse(mockFinanceData));
            });
        });

        it('should render page heading "Finance"', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
            });
        });

        it('should display balance when data is loaded', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByText('Zůstatek')).toBeInTheDocument();
                expect(screen.getByText(/1500/)).toBeInTheDocument();
            });
        });

        it('should render transaction table with real HalEmbeddedTable', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByRole('table')).toBeInTheDocument();
            });
        });

        it('should render deposit button through real HalFormButton', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByRole('button', {name: /deposit/i})).toBeInTheDocument();
            });
        });
    });

    describe('Subresource Integration', () => {
        beforeEach(() => {
            // Mock implementation to handle multiple endpoints and query parameters
            fetchSpy.mockImplementation((url: string) => {
                // Remove query parameters for endpoint matching
                const baseUrl = url.split('?')[0];

                if (baseUrl.endsWith('/transactions') || baseUrl.includes('/transactions/')) {
                    return Promise.resolve(createMockResponse(mockTransactionData));
                }

                // Default response for finance data
                return Promise.resolve(createMockResponse(mockFinanceData));
            });
        });

        it('should load and display finance data through real child components', async () => {
            renderPage(<MemberFinancePage/>);

            // Verify all real child components render with actual data
            await waitFor(() => {
                expect(screen.getByText('Finance')).toBeInTheDocument();
                expect(screen.getByText('Zůstatek')).toBeInTheDocument();
                expect(screen.getByRole('table')).toBeInTheDocument();
                expect(screen.getByRole('button', {name: /deposit/i})).toBeInTheDocument();
            });
        });
    });

    describe('Error Handling', () => {
        it('should display page heading when API fetch fails with HTTP 500', async () => {
            fetchSpy.mockRejectedValueOnce(new Error('HTTP 500: Internal Server Error'));

            renderPage(<MemberFinancePage/>);

            // Page should still render heading and gracefully handle error
            await waitFor(() => {
                expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
            });
        });

        it('should display page heading when API fetch fails with network error', async () => {
            fetchSpy.mockRejectedValueOnce(new Error('Network error'));

            renderPage(<MemberFinancePage/>);

            // Page should still render and not crash
            await waitFor(() => {
                expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
            });
        });

        it('should render page structure while data is loading', async () => {
            fetchSpy.mockImplementationOnce(() => new Promise(resolve => {
                // Simulate a slow response
                setTimeout(() => resolve(createMockResponse(mockFinanceData)), 100);
            }));

            const {container} = renderPage(<MemberFinancePage/>);

            // Page should render and show loading state without crashing
            expect(container).toBeInTheDocument();

            // Eventually loads the Finance heading
            await waitFor(() => {
                expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
            }, {timeout: 1000});
        });
    });

    describe('Null Safety and Missing Data', () => {
        it('should render page with default values when balance is missing', async () => {
            const financeDataWithoutBalance = mockFinanceResource({
                balance: undefined,
                _links: {
                    self: {href: 'https://test.com/api/finances/123'},
                    transactions: {href: 'https://test.com/api/finances/123/transactions'},
                },
            });

            fetchSpy.mockImplementation((url: string) => {
                const baseUrl = url.split('?')[0];
                if (baseUrl.endsWith('/transactions') || baseUrl.includes('/transactions/')) {
                    return Promise.resolve(createMockResponse(mockTransactionData));
                }
                return Promise.resolve(createMockResponse(financeDataWithoutBalance));
            });

            renderPage(<MemberFinancePage/>);

            // Page should render heading and handle missing balance gracefully
            await waitFor(() => {
                expect(screen.getByText('Finance')).toBeInTheDocument();
            });
        });

        it('should render page when transactions link is missing', async () => {
            const financeDataWithoutTransactionsLink = mockFinanceResource({
                balance: 1500,
                _links: {
                    self: {href: 'https://test.com/api/finances/123'},
                },
            });

            fetchSpy.mockResolvedValueOnce(createMockResponse(financeDataWithoutTransactionsLink));

            renderPage(<MemberFinancePage/>);

            // Page should render without crashing even if transactions link is missing
            await waitFor(() => {
                expect(screen.getByText('Finance')).toBeInTheDocument();
            });
        });

        it('should render page when resource returns HTTP 404', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse({}, 404));

            renderPage(<MemberFinancePage/>);

            // Page should render heading and handle 404 gracefully
            await waitFor(() => {
                expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
            });
        });
    });
});

describe('MemberName Component', () => {
    let queryClient: QueryClient;
    let fetchSpy: Mock;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        vi.clearAllMocks();
        // Mock global fetch
        fetchSpy = vi.fn() as Mock;
        (globalThis as any).fetch = fetchSpy;
    });

    afterEach(() => {
        delete (globalThis as any).fetch;
    });

    // MemberName uses useHalRoute internally, so it needs HalRouteProvider
    const renderComponent = (ui: React.ReactElement) => {
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/members/123']}>
                    <HalRouteProvider>
                        {ui}
                    </HalRouteProvider>
                </MemoryRouter>
            </QueryClientProvider>
        );
    };

    describe('Name Display', () => {
        it('should display firstName and lastName when provided', () => {
            const user = mockMemberResource();
            renderComponent(<MemberName user={user as any}/>);
            expect(screen.getByText(`${user.firstName} ${user.lastName}`)).toBeInTheDocument();
        });

        it('should handle user object with partial data', () => {
            const user = {firstName: 'Jan'} as any;
            renderComponent(<MemberName user={user}/>);
            // Should display firstName and '-' for lastName
            expect(screen.getByText(/Jan.*-/)).toBeInTheDocument();
        });

        it('should display "- lastName" when firstName is missing', () => {
            const user = {firstName: undefined, lastName: 'Novák'} as any;
            renderComponent(<MemberName user={user}/>);
            expect(screen.getByText('- Novák')).toBeInTheDocument();
        });

        it('should display "firstName -" when lastName is missing', () => {
            const user = {firstName: 'Jan', lastName: undefined} as any;
            renderComponent(<MemberName user={user}/>);
            expect(screen.getByText('Jan -')).toBeInTheDocument();
        });

        it('should display "- -" when both names are missing', () => {
            const user = {} as any;
            renderComponent(<MemberName user={user}/>);
            expect(screen.getByText('- -')).toBeInTheDocument();
        });

        it('should handle empty string names', () => {
            const user = {firstName: '', lastName: ''} as any;
            const {container} = renderComponent(<MemberName user={user}/>);
            // Component should render without error
            const span = container.querySelector('span');
            expect(span).toBeInTheDocument();
        });

        it('should handle whitespace-only names', () => {
            const user = {firstName: '   ', lastName: '   '} as any;
            const {container} = renderComponent(<MemberName user={user}/>);
            // Component should render without error
            const span = container.querySelector('span');
            expect(span).toBeInTheDocument();
        });

        it('should handle special characters in names', () => {
            const user = {firstName: 'José', lastName: 'García-López'} as any;
            renderComponent(<MemberName user={user}/>);
            expect(screen.getByText('José García-López')).toBeInTheDocument();
        });

        it('should handle very long names', () => {
            const user = {
                firstName: 'Alexander'.repeat(5),
                lastName: 'Schwarz'.repeat(5),
            } as any;
            renderComponent(<MemberName user={user}/>);
            expect(screen.getByText(new RegExp(`${user.firstName}.*${user.lastName}`))).toBeInTheDocument();
        });
    });

    describe('Rendering', () => {
        it('should render as span element', () => {
            const user = mockMemberResource();
            const {container} = renderComponent(<MemberName user={user as any}/>);
            const span = container.querySelector('span');
            expect(span).toBeInTheDocument();
        });

        it('should display full name with space between first and last name', () => {
            const user = mockMemberResource({firstName: 'Jan', lastName: 'Novák'});
            renderComponent(<MemberName user={user as any}/>);
            const text = screen.getByText('Jan Novák');
            expect(text.textContent).toBe('Jan Novák');
        });
    });
});
