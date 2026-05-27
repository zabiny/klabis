import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import {MemberAccountManagePage} from './MemberAccountManagePage.tsx';
import {createMockResponse} from '../../__mocks__/mockFetch.ts';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {type Mock, vi} from 'vitest';
import {HalFormProvider} from '../../contexts/HalFormContext.tsx';

vi.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

const mockAccountDataWithAffordances = {
    memberId: 'member-456',
    balance: 750,
    currency: 'CZK',
    _links: {
        self: {href: 'https://test.com/api/members/456/account'},
        transactions: {href: 'https://test.com/api/members/456/account/transactions'},
        member: {href: 'https://test.com/api/members/456'},
    },
    _templates: {
        deposit: {
            method: 'POST',
            target: 'https://test.com/api/members/456/account/transactions',
            title: 'Vložit',
            properties: [
                {name: 'amount', type: 'number', required: true},
                {name: 'occurredAt', type: 'date'},
                {name: 'note', type: 'text'},
            ],
        },
        charge: {
            method: 'POST',
            target: 'https://test.com/api/members/456/account/transactions/charge',
            title: 'Strhnout',
            properties: [
                {name: 'amount', type: 'number', required: true},
                {name: 'occurredAt', type: 'date'},
                {name: 'note', type: 'text'},
            ],
        },
    },
};

const mockAccountDataWithoutAffordances = {
    memberId: 'member-789',
    balance: 200,
    currency: 'CZK',
    _links: {
        self: {href: 'https://test.com/api/members/789/account'},
        transactions: {href: 'https://test.com/api/members/789/account/transactions'},
        member: {href: 'https://test.com/api/members/789'},
    },
};

const mockTransactionData = {
    _embedded: {
        transactions: [
            {
                id: 'tx-1',
                occurredAt: '2025-03-10',
                amount: 500,
                currency: 'CZK',
                note: 'Initial deposit',
                type: 'DEPOSIT',
                reversesTransactionId: null,
                _links: {
                    self: {href: 'https://test.com/api/members/456/account/transactions/tx-1'},
                },
            },
        ],
    },
    page: {totalElements: 1, totalPages: 1, size: 20, number: 0},
    _links: {self: {href: 'https://test.com/api/members/456/account/transactions'}},
};

describe('MemberAccountManagePage', () => {
    let queryClient: QueryClient;
    let fetchSpy: Mock;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {queries: {retry: false, gcTime: 0}},
        });
        vi.clearAllMocks();
        fetchSpy = vi.fn() as Mock;
        (globalThis as any).fetch = fetchSpy;
    });

    afterEach(() => {
        delete (globalThis as any).fetch;
    });

    const renderPage = (ui: React.ReactElement, initialRoute = '/members/456/account') => {
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

    describe('With FINANCE:MANAGE affordances', () => {
        beforeEach(() => {
            fetchSpy.mockImplementation((url: string) => {
                const baseUrl = url.split('?')[0];
                if (baseUrl.endsWith('/transactions') || baseUrl.includes('/transactions/')) {
                    return Promise.resolve(createMockResponse(mockTransactionData));
                }
                return Promise.resolve(createMockResponse(mockAccountDataWithAffordances));
            });
        });

        it('should render page heading "Účet člena"', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.getByRole('heading', {name: /Účet člena/i})).toBeInTheDocument();
            });
        });

        it('should display balance value', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.getByText(/750/)).toBeInTheDocument();
            });
        });

        it('should render Deposit button when deposit affordance is present', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.getByRole('button', {name: /Vložit/i})).toBeInTheDocument();
            });
        });

        it('should render Charge button when charge affordance is present', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.getByRole('button', {name: /Strhnout/i})).toBeInTheDocument();
            });
        });
    });

    describe('Without FINANCE:MANAGE affordances (own account view)', () => {
        beforeEach(() => {
            fetchSpy.mockImplementation((url: string) => {
                const baseUrl = url.split('?')[0];
                if (baseUrl.endsWith('/transactions') || baseUrl.includes('/transactions/')) {
                    return Promise.resolve(createMockResponse(mockTransactionData));
                }
                return Promise.resolve(createMockResponse(mockAccountDataWithoutAffordances));
            });
        });

        it('should NOT render Deposit button when deposit affordance is absent', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.queryByRole('button', {name: /Vložit/i})).not.toBeInTheDocument();
            });
        });

        it('should NOT render Charge button when charge affordance is absent', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.queryByRole('button', {name: /Strhnout/i})).not.toBeInTheDocument();
            });
        });

        it('should still render balance', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.getByText(/200/)).toBeInTheDocument();
            });
        });
    });
});
