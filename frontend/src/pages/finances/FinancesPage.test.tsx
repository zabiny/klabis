import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import {createMockResponse} from '../../__mocks__/mockFetch.ts';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {type Mock, vi} from 'vitest';
import {HalFormProvider} from '../../contexts/HalFormContext.tsx';
import {TransactionsTable} from './FinancesPage.tsx';
import {labels} from '../../localization';


const mockMemberData = {
    firstName: 'Pavel',
    lastName: 'Procházka',
    registrationNumber: 'ZBM5678',
    _links: {self: {href: 'https://test.com/api/members/99'}},
};

const mockTransactionWithRecordedBy = {
    _embedded: {
        transactions: [
            {
                id: 'tx-1',
                occurredAt: '2025-03-10',
                amount: 500,
                currency: 'CZK',
                note: 'Test deposit',
                type: 'DEPOSIT',
                reversesTransactionId: null,
                _links: {
                    self: {href: 'https://test.com/api/members/456/account/transactions/tx-1'},
                    recordedBy: {href: 'https://test.com/api/members/99'},
                },
            },
        ],
    },
    page: {totalElements: 1, totalPages: 1, size: 20, number: 0},
    _links: {self: {href: 'https://test.com/api/members/456/account/transactions'}},
};

const mockTransactionWithoutRecordedBy = {
    _embedded: {
        transactions: [
            {
                id: 'tx-2',
                occurredAt: '2025-03-11',
                amount: 200,
                currency: 'CZK',
                note: 'No recorder',
                type: 'DEPOSIT',
                reversesTransactionId: null,
                _links: {
                    self: {href: 'https://test.com/api/members/456/account/transactions/tx-2'},
                },
            },
        ],
    },
    page: {totalElements: 1, totalPages: 1, size: 20, number: 0},
    _links: {self: {href: 'https://test.com/api/members/456/account/transactions'}},
};

const mockAccountData = {
    memberId: 'member-456',
    balance: 750,
    currency: 'CZK',
    _links: {
        self: {href: 'https://test.com/api/members/456/account/transactions'},
    },
};

describe('TransactionsTable - Zaznamenal column', () => {
    let queryClient: QueryClient;
    let fetchSpy: Mock;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {queries: {retry: false, gcTime: 0}},
        });
        vi.clearAllMocks();
        fetchSpy = vi.fn() as Mock;
        (globalThis as typeof globalThis & Record<string, unknown>).fetch = fetchSpy;
    });

    afterEach(() => {
        Reflect.deleteProperty(globalThis, 'fetch');
    });

    const renderTable = (ui: React.ReactElement, initialRoute = '/members/456/account/transactions') => {
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

    it('should render "Zaznamenal" column header', async () => {
        fetchSpy.mockImplementation((url: string) => {
            const baseUrl = url.split('?')[0];
            if (baseUrl.includes('/transactions')) {
                return Promise.resolve(createMockResponse(mockTransactionWithRecordedBy));
            }
            return Promise.resolve(createMockResponse(mockAccountData));
        });

        renderTable(<TransactionsTable />);

        await waitFor(() => {
            expect(screen.getByText(labels.finance.recordedBy)).toBeInTheDocument();
        });
    });

    it('should render member name when recordedBy link is present', async () => {
        fetchSpy.mockImplementation((url: string) => {
            const baseUrl = url.split('?')[0];
            if (baseUrl.endsWith('/members/99')) {
                return Promise.resolve(createMockResponse(mockMemberData));
            }
            if (baseUrl.includes('/transactions')) {
                return Promise.resolve(createMockResponse(mockTransactionWithRecordedBy));
            }
            return Promise.resolve(createMockResponse(mockAccountData));
        });

        renderTable(<TransactionsTable />);

        await waitFor(() => {
            expect(screen.getByText('Pavel Procházka')).toBeInTheDocument();
        });
    });

    it('should render dash "—" when recordedBy link is missing', async () => {
        fetchSpy.mockImplementation((url: string) => {
            const baseUrl = url.split('?')[0];
            if (baseUrl.includes('/transactions')) {
                return Promise.resolve(createMockResponse(mockTransactionWithoutRecordedBy));
            }
            return Promise.resolve(createMockResponse(mockAccountData));
        });

        renderTable(<TransactionsTable />);

        await waitFor(() => {
            expect(screen.getByText('—')).toBeInTheDocument();
        });
    });

    it('should render dash "—" when recordedBy lookup fails with 404', async () => {
        fetchSpy.mockImplementation((url: string) => {
            const baseUrl = url.split('?')[0];
            if (baseUrl.endsWith('/members/99')) {
                return Promise.resolve(createMockResponse(null, 404));
            }
            if (baseUrl.includes('/transactions')) {
                return Promise.resolve(createMockResponse(mockTransactionWithRecordedBy));
            }
            return Promise.resolve(createMockResponse(mockAccountData));
        });

        renderTable(<TransactionsTable />);

        await waitFor(() => {
            expect(screen.getByText('—')).toBeInTheDocument();
        });
    });
});
