import '@testing-library/jest-dom';
import {render, screen, waitFor, fireEvent} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import {FinanceTransactionDialog} from './FinanceTransactionDialog.tsx';
import {createMockResponse, createDelayedMockResponse} from '../../__mocks__/mockFetch.ts';
import {type Mock, vi} from 'vitest';
import type {Link} from '../../api/types.ts';

const ACCOUNT_LINK: Link = {href: 'https://test.com/api/members/456/account'};
const ACCOUNT_OWNER_HREF = 'https://test.com/api/members/456';

const mockAccountBothTemplates = {
    balance: 750,
    currency: 'CZK',
    _links: {
        self: {href: ACCOUNT_LINK.href},
        transactions: {href: 'https://test.com/api/members/456/account/transactions'},
        accountOwner: {href: ACCOUNT_OWNER_HREF},
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

const mockAccountDepositOnly = {
    ...mockAccountBothTemplates,
    _templates: {
        deposit: mockAccountBothTemplates._templates.deposit,
    },
};

const mockAccountChargeOnly = {
    ...mockAccountBothTemplates,
    _templates: {
        charge: mockAccountBothTemplates._templates.charge,
    },
};

const mockAccountNoTemplates = {
    balance: 750,
    currency: 'CZK',
    _links: {
        self: {href: ACCOUNT_LINK.href},
        accountOwner: {href: ACCOUNT_OWNER_HREF},
    },
};

const mockMember = {
    firstName: 'Jan',
    lastName: 'Novák',
    registrationNumber: 'ZBM8102',
    _links: {
        self: {href: ACCOUNT_OWNER_HREF},
    },
};

describe('FinanceTransactionDialog', () => {
    let queryClient: QueryClient;
    let fetchSpy: Mock;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {queries: {retry: false, gcTime: 0}},
        });
        fetchSpy = vi.fn() as Mock;
        (globalThis as any).fetch = fetchSpy;
        localStorage.clear();
    });

    afterEach(() => {
        delete (globalThis as any).fetch;
    });

    const renderDialog = (props: Partial<React.ComponentProps<typeof FinanceTransactionDialog>> = {}) => {
        const defaultProps = {
            accountLink: ACCOUNT_LINK,
            isOpen: true,
            onClose: vi.fn(),
        };
        return render(
            <QueryClientProvider client={queryClient}>
                <FinanceTransactionDialog {...defaultProps} {...props} />
            </QueryClientProvider>
        );
    };

    const setupFetch = (accountData = mockAccountBothTemplates, memberData = mockMember) => {
        fetchSpy.mockImplementation((url: string) => {
            if (url.includes('/members/456/account') && !url.includes('/transactions')) {
                return Promise.resolve(createMockResponse(accountData));
            }
            if (url.includes('/members/456') && !url.includes('/account')) {
                return Promise.resolve(createMockResponse(memberData));
            }
            return Promise.resolve(createMockResponse({}));
        });
    };

    // 3.1a: Skeleton shown while both fetches are pending
    it('3.1a: shows skeleton while both fetches are pending', async () => {
        fetchSpy.mockImplementation(() => createDelayedMockResponse(mockAccountBothTemplates, 200));
        renderDialog();

        expect(screen.getByTestId('finance-dialog-skeleton')).toBeInTheDocument();
    });

    // 3.1b: After resolve shows header with member name, registration number, balance
    it('3.1b: renders header with firstName, lastName, registrationNumber, and balance after both fetches resolve', async () => {
        setupFetch();
        renderDialog();

        await waitFor(() => {
            expect(screen.queryByTestId('finance-dialog-skeleton')).not.toBeInTheDocument();
        });
        expect(screen.getByText(/Jan/)).toBeInTheDocument();
        expect(screen.getByText(/Novák/)).toBeInTheDocument();
        expect(screen.getByText(/ZBM8102/)).toBeInTheDocument();
        expect(screen.getByText(/750/)).toBeInTheDocument();
    });

    // 3.1c: Both templates → both tabs shown
    it('3.1c: shows both tabs when both deposit and charge templates are present', async () => {
        setupFetch(mockAccountBothTemplates);
        renderDialog();

        await waitFor(() => {
            expect(screen.getByRole('tab', {name: /Připsání vkladu/i})).toBeInTheDocument();
        });
        expect(screen.getByRole('tab', {name: /Stržení částky/i})).toBeInTheDocument();
    });

    // 3.1d: Only deposit template → no tabs, just form
    it('3.1d: hides tabs and shows only deposit form when only deposit template is present', async () => {
        setupFetch(mockAccountDepositOnly);
        renderDialog();

        // Wait for skeleton to disappear (both fetches resolved)
        await waitFor(() => {
            expect(screen.queryByTestId('finance-dialog-skeleton')).not.toBeInTheDocument();
        });
        // No tabs shown when only one template present
        expect(screen.queryByRole('tab')).not.toBeInTheDocument();
        // Form for deposit should still be rendered
        expect(screen.getByLabelText(/Částka/i)).toBeInTheDocument();
    });

    // 3.1e: No templates → fallback message
    it('3.1e: shows fallback message when no templates are present', async () => {
        setupFetch(mockAccountNoTemplates);
        renderDialog();

        await waitFor(() => {
            expect(screen.getByText(/Žádná operace není povolena/i)).toBeInTheDocument();
        });
        expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    });

    // 3.2: Shared state — values preserved when switching tabs
    it('3.2: preserves amount and note values when switching between tabs', async () => {
        setupFetch(mockAccountBothTemplates);
        renderDialog();

        await waitFor(() => {
            expect(screen.getByRole('tab', {name: /Připsání vkladu/i})).toBeInTheDocument();
        });

        const amountInput = screen.getByLabelText(/Částka/i);
        const noteInput = screen.getByLabelText(/Poznámka/i);

        await userEvent.clear(amountInput);
        await userEvent.type(amountInput, '500');
        await userEvent.clear(noteInput);
        await userEvent.type(noteInput, 'Test note');

        // Switch to charge tab
        await userEvent.click(screen.getByRole('tab', {name: /Stržení částky/i}));

        // Values should be preserved
        expect(screen.getByLabelText(/Částka/i)).toHaveValue(500);
        expect(screen.getByLabelText(/Poznámka/i)).toHaveValue('Test note');
    });

    // 3.3: localStorage persistence of active tab
    it('3.3: restores last selected tab from localStorage on open', async () => {
        localStorage.setItem('klabis.financeDialog.lastTab', 'charge');
        setupFetch(mockAccountBothTemplates);
        renderDialog();

        await waitFor(() => {
            expect(screen.getByRole('tab', {name: /Stržení částky/i})).toBeInTheDocument();
        });

        // Charge tab should be active (has active styling indicator)
        const chargeTab = screen.getByRole('tab', {name: /Stržení částky/i});
        expect(chargeTab).toHaveAttribute('aria-selected', 'true');
    });

    it('3.3b: saves selected tab to localStorage when switching', async () => {
        setupFetch(mockAccountBothTemplates);
        renderDialog();

        await waitFor(() => {
            expect(screen.getByRole('tab', {name: /Stržení částky/i})).toBeInTheDocument();
        });

        await userEvent.click(screen.getByRole('tab', {name: /Stržení částky/i}));

        expect(localStorage.getItem('klabis.financeDialog.lastTab')).toBe('charge');
    });

    it('3.3c: defaults to deposit tab when no localStorage value is set', async () => {
        setupFetch(mockAccountBothTemplates);
        renderDialog();

        await waitFor(() => {
            expect(screen.getByRole('tab', {name: /Připsání vkladu/i})).toBeInTheDocument();
        });

        const depositTab = screen.getByRole('tab', {name: /Připsání vkladu/i});
        expect(depositTab).toHaveAttribute('aria-selected', 'true');
    });

    // 3.4: Submit posts to correct template endpoint, invokes onClose, invalidates queries
    it('3.4: submits POST to deposit template target and invokes onClose on success', async () => {
        setupFetch(mockAccountBothTemplates);
        fetchSpy.mockImplementation((url: string, options?: RequestInit) => {
            if (url.includes('/members/456/account') && !url.includes('/transactions') && options?.method === undefined) {
                return Promise.resolve(createMockResponse(mockAccountBothTemplates));
            }
            if (url.includes('/members/456') && !url.includes('/account') && options?.method === undefined) {
                return Promise.resolve(createMockResponse(mockMember));
            }
            // POST to deposit endpoint
            if (url.includes('/transactions') && options?.method === 'POST') {
                return Promise.resolve(createMockResponse({}, 200));
            }
            return Promise.resolve(createMockResponse({}));
        });

        const onClose = vi.fn();
        renderDialog({onClose});

        await waitFor(() => {
            expect(screen.getByLabelText(/Částka/i)).toBeInTheDocument();
        });

        const amountInput = screen.getByLabelText(/Částka/i);
        await userEvent.clear(amountInput);
        await userEvent.type(amountInput, '100');

        const submitBtn = screen.getByRole('button', {name: /Připsání vkladu/i});
        await userEvent.click(submitBtn);

        await waitFor(() => {
            expect(onClose).toHaveBeenCalled();
        });

        // Verify POST was made to the deposit target
        const postCalls = fetchSpy.mock.calls.filter(
            ([, options]: [string, RequestInit?]) => options?.method === 'POST'
        );
        expect(postCalls.length).toBeGreaterThan(0);
        expect(postCalls[0][0]).toContain('/transactions');
    });

    it('3.4b: submits POST to charge template target when charge tab is active', async () => {
        fetchSpy.mockImplementation((url: string, options?: RequestInit) => {
            if (url.includes('/members/456/account') && !url.includes('/transactions') && !options?.method) {
                return Promise.resolve(createMockResponse(mockAccountBothTemplates));
            }
            if (url.includes('/members/456') && !url.includes('/account') && !options?.method) {
                return Promise.resolve(createMockResponse(mockMember));
            }
            if (options?.method === 'POST') {
                return Promise.resolve(createMockResponse({}, 200));
            }
            return Promise.resolve(createMockResponse({}));
        });

        const onClose = vi.fn();
        renderDialog({onClose});

        await waitFor(() => {
            expect(screen.getByRole('tab', {name: /Stržení částky/i})).toBeInTheDocument();
        });

        // Switch to charge tab
        await userEvent.click(screen.getByRole('tab', {name: /Stržení částky/i}));

        const amountInput = screen.getByLabelText(/Částka/i);
        await userEvent.clear(amountInput);
        await userEvent.type(amountInput, '200');

        const submitBtn = screen.getByRole('button', {name: /Stržení částky/i});
        await userEvent.click(submitBtn);

        await waitFor(() => {
            expect(onClose).toHaveBeenCalled();
        });

        const postCalls = fetchSpy.mock.calls.filter(
            ([, options]: [string, RequestInit?]) => options?.method === 'POST'
        );
        expect(postCalls.length).toBeGreaterThan(0);
        expect(postCalls[0][0]).toContain('/transactions/charge');
    });

    // Dialog not rendered when isOpen=false
    it('does not render dialog content when isOpen is false', () => {
        setupFetch();
        renderDialog({isOpen: false});
        expect(screen.queryByTestId('finance-dialog-skeleton')).not.toBeInTheDocument();
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    // CTA button label and variant
    it('shows green CTA button for deposit tab and red CTA for charge tab', async () => {
        setupFetch(mockAccountBothTemplates);
        renderDialog();

        await waitFor(() => {
            expect(screen.getByRole('tab', {name: /Připsání vkladu/i})).toBeInTheDocument();
        });

        // Check deposit CTA
        const depositCta = screen.getByRole('button', {name: /Připsání vkladu/i});
        expect(depositCta).toHaveClass('bg-emerald-600');

        // Switch to charge
        await userEvent.click(screen.getByRole('tab', {name: /Stržení částky/i}));

        const chargeCta = screen.getByRole('button', {name: /Stržení částky/i});
        expect(chargeCta).toHaveClass('bg-red-600');
    });

    // Cancel button invokes onClose
    it('invokes onClose when Cancel button is clicked', async () => {
        setupFetch();
        const onClose = vi.fn();
        renderDialog({onClose});

        await waitFor(() => {
            expect(screen.getByRole('button', {name: /Zrušit/i})).toBeInTheDocument();
        });

        await userEvent.click(screen.getByRole('button', {name: /Zrušit/i}));
        expect(onClose).toHaveBeenCalled();
    });
});
