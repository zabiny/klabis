import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import {MemberFinancePage} from './FinancesPage.tsx';
import {createMockResponse} from '../../__mocks__/mockFetch.ts';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {type Mock, vi} from 'vitest';
import {HalFormProvider} from "../../contexts/HalFormContext.tsx";

vi.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

const mockAccountData = {
    memberId: 'member-123',
    balance: 1500,
    currency: 'CZK',
    _links: {
        self: {href: 'https://test.com/api/members/123/account'},
        transactions: {href: 'https://test.com/api/members/123/account/transactions'},
        member: {href: 'https://test.com/api/members/123'},
    },
};

const mockTransactionData = {
    _embedded: {
        transactions: [
            {
                id: 'tx-1',
                occurredAt: '2025-01-15',
                amount: 500,
                currency: 'CZK',
                note: 'Monthly deposit',
                type: 'DEPOSIT',
                reversesTransactionId: null,
                _links: {
                    self: {href: 'https://test.com/api/members/123/account/transactions/tx-1'},
                },
            },
            {
                id: 'tx-2',
                occurredAt: '2025-01-10',
                amount: -100,
                currency: 'CZK',
                note: 'Charge',
                type: 'OTHER',
                reversesTransactionId: null,
                _links: {
                    self: {href: 'https://test.com/api/members/123/account/transactions/tx-2'},
                },
            },
        ],
    },
    page: {
        totalElements: 2,
        totalPages: 1,
        size: 20,
        number: 0,
    },
    _links: {
        self: {href: 'https://test.com/api/members/123/account/transactions'},
    },
};

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
        fetchSpy = vi.fn() as Mock;
        (globalThis as any).fetch = fetchSpy;
    });

    afterEach(() => {
        delete (globalThis as any).fetch;
    });

    const renderPage = (ui: React.ReactElement, initialRoute = '/members/123/account') => {
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={[initialRoute]}>
                    <HalFormProvider>
                        <HalRouteProvider>
                            {ui}
                        </HalRouteProvider>
                    </HalFormProvider>
                </MemoryRouter>
            </QueryClientProvider>
        );
    };

    describe('Data Loading and Display', () => {
        beforeEach(() => {
            fetchSpy.mockImplementation((url: string) => {
                const baseUrl = url.split('?')[0];
                if (baseUrl.endsWith('/transactions') || baseUrl.includes('/transactions/')) {
                    return Promise.resolve(createMockResponse(mockTransactionData));
                }
                return Promise.resolve(createMockResponse(mockAccountData));
            });
        });

        it('should render page heading "Finance"', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
            });
        });

        it('should display Zůstatek label', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByText('Zůstatek')).toBeInTheDocument();
            });
        });

        it('should display balance value when data is loaded', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByText(/1\s*500/)).toBeInTheDocument();
            });
        });

        it('should render transaction history section heading', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByRole('heading', {name: /Historie transakcí/i})).toBeInTheDocument();
            });
        });

        it('should render filter bar with type filter options', async () => {
            renderPage(<MemberFinancePage/>);
            await waitFor(() => {
                expect(screen.getByText('Vše')).toBeInTheDocument();
                expect(screen.getByText('Vklad')).toBeInTheDocument();
                expect(screen.getByText('Výdaj')).toBeInTheDocument();
            });
        });
    });

    describe('Balance display', () => {
        it('should show negative balance in red styling when balance is negative', async () => {
            const negativeAccountData = {...mockAccountData, balance: -250};
            fetchSpy.mockImplementation((url: string) => {
                const baseUrl = url.split('?')[0];
                if (baseUrl.endsWith('/transactions')) {
                    return Promise.resolve(createMockResponse(mockTransactionData));
                }
                return Promise.resolve(createMockResponse(negativeAccountData));
            });

            renderPage(<MemberFinancePage/>);

            await waitFor(() => {
                const balanceEl = screen.getByText(/-250/);
                expect(balanceEl).toHaveClass('text-red-600');
            });
        });

        it('should show zero or positive balance in green styling', async () => {
            fetchSpy.mockImplementation((url: string) => {
                const baseUrl = url.split('?')[0];
                if (baseUrl.endsWith('/transactions')) {
                    return Promise.resolve(createMockResponse(mockTransactionData));
                }
                return Promise.resolve(createMockResponse(mockAccountData));
            });

            renderPage(<MemberFinancePage/>);

            await waitFor(() => {
                const balanceEl = screen.getByText(/1\s*500/);
                expect(balanceEl).toHaveClass('text-green-700');
            });
        });
    });

    describe('Error Handling', () => {
        it('should display page heading when API fetch fails with HTTP 500', async () => {
            fetchSpy.mockRejectedValueOnce(new Error('HTTP 500: Internal Server Error'));

            renderPage(<MemberFinancePage/>);

            await waitFor(() => {
                expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
            });
        });

        it('should render page structure while data is loading', async () => {
            fetchSpy.mockImplementationOnce(() => new Promise(resolve => {
                setTimeout(() => resolve(createMockResponse(mockAccountData)), 100);
            }));

            const {container} = renderPage(<MemberFinancePage/>);
            expect(container).toBeInTheDocument();

            await waitFor(() => {
                expect(screen.getByRole('heading', {name: /Finance/i})).toBeInTheDocument();
            }, {timeout: 1000});
        });
    });

    describe('Missing data handling', () => {
        it('should render page with fallback when balance is missing', async () => {
            const noBalanceData = {...mockAccountData, balance: undefined};
            fetchSpy.mockImplementation((url: string) => {
                const baseUrl = url.split('?')[0];
                if (baseUrl.endsWith('/transactions')) {
                    return Promise.resolve(createMockResponse(mockTransactionData));
                }
                return Promise.resolve(createMockResponse(noBalanceData));
            });

            renderPage(<MemberFinancePage/>);

            await waitFor(() => {
                expect(screen.getByText('Finance')).toBeInTheDocument();
                expect(screen.getByText('Zůstatek')).toBeInTheDocument();
            });
        });
    });
});
