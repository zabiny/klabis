import '@testing-library/jest-dom';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import {MemberAccountManagePage} from './MemberAccountManagePage.tsx';
import {createMockResponse} from '../../__mocks__/mockFetch.ts';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {type Mock, vi} from 'vitest';
import {HalFormProvider} from '../../contexts/HalFormContext.tsx';
import type {Link} from '../../api/types.ts';

vi.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

vi.mock('../../components/finance/FinanceTransactionDialog.tsx', () => ({
    FinanceTransactionDialog: ({
        accountLink,
        isOpen,
        onClose,
    }: {
        accountLink: Link;
        isOpen: boolean;
        onClose: () => void;
    }) => {
        if (!isOpen) return null;
        return (
            <div data-testid="finance-transaction-dialog">
                <span data-testid="dialog-account-href">{accountLink.href}</span>
                <button onClick={onClose}>Close dialog</button>
            </div>
        );
    },
}));

const mockOwnerData = {
    firstName: 'Jan',
    lastName: 'Novák',
    registrationNumber: 'ZBM1234',
    _links: {self: {href: 'https://test.com/api/members/456'}},
};

const mockAccountDataWithAffordances = {
    memberId: 'member-456',
    balance: 750,
    currency: 'CZK',
    _links: {
        self: {href: 'https://test.com/api/members/456/account'},
        transactions: {href: 'https://test.com/api/members/456/account/transactions'},
        member: {href: 'https://test.com/api/members/456'},
        accountOwner: {href: 'https://test.com/api/members/456'},
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
        (globalThis as typeof globalThis & Record<string, unknown>).fetch = fetchSpy;
    });

    afterEach(() => {
        Reflect.deleteProperty(globalThis, 'fetch');
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
                if (baseUrl.endsWith('/api/members/456') || baseUrl === 'https://test.com/api/members/456') {
                    return Promise.resolve(createMockResponse(mockOwnerData));
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

        it('should render a single "Vložit / Vybrat" action button when manager affordances are present', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.getByRole('button', {name: /Provést finanční transakci/i})).toBeInTheDocument();
            });
        });

        it('should render action button with visible label text "Vložit / Vybrat"', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.getByText('Vložit / Vybrat')).toBeInTheDocument();
            });
        });

        it('should render action button with correct aria-label', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                const button = screen.getByRole('button', {name: /Provést finanční transakci/i});
                expect(button).toHaveAttribute('aria-label', 'Provést finanční transakci');
            });
        });

        it('should NOT render separate Deposit or Charge buttons', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.queryByRole('button', {name: /^Vložit$/i})).not.toBeInTheDocument();
                expect(screen.queryByRole('button', {name: /^Strhnout$/i})).not.toBeInTheDocument();
            });
        });

        it('should open FinanceTransactionDialog with account self link when action button is clicked', async () => {
            renderPage(<MemberAccountManagePage/>);

            const button = await screen.findByRole('button', {name: /Provést finanční transakci/i});
            fireEvent.click(button);

            await waitFor(() => {
                expect(screen.getByTestId('finance-transaction-dialog')).toBeInTheDocument();
                expect(screen.getByTestId('dialog-account-href')).toHaveTextContent(
                    'https://test.com/api/members/456/account'
                );
            });
        });

        it('should display account owner name and registration number above balance card', async () => {
            renderPage(<MemberAccountManagePage/>);

            await waitFor(() => {
                expect(screen.getByText('Jan Novák (ZBM1234)')).toBeInTheDocument();
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

        it('should NOT render account owner section when accountOwner link is missing', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.queryByTestId('account-owner-header')).not.toBeInTheDocument();
            });
        });

        it('should NOT render "Vložit / Vybrat" button when no manager affordances are present', async () => {
            renderPage(<MemberAccountManagePage/>);
            await waitFor(() => {
                expect(screen.queryByRole('button', {name: /Provést finanční transakci/i})).not.toBeInTheDocument();
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
