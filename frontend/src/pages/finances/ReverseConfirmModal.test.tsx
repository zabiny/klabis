import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ApiError} from '../../api/apiError';
import {labels} from '../../localization';
import {ReverseConfirmModal} from './ReverseConfirmModal';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedMutation: vi.fn(),
}));
vi.mock('../../contexts/toastContext', () => ({
    useToast: () => ({addToast: vi.fn()}),
}));

import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';

const transaction = {
    reverseTarget: 'https://test.com/api/members/1/account/transactions/9/reverse',
    txId: '9',
    amount: 250,
    currency: 'CZK',
    occurredAt: '2026-01-05',
    note: null,
    type: 'DEPOSIT',
};

const failWith = (error: Error) => {
    vi.mocked(useAuthorizedMutation).mockReturnValue({
        mutate: vi.fn().mockImplementation((_vars, opts) => opts?.onError?.(error)),
        isPending: false,
    } as unknown as ReturnType<typeof useAuthorizedMutation>);
};

describe('ReverseConfirmModal', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows the Czech text of an already reversed transaction', async () => {
        failWith(new ApiError(409, {
            type: '/errors/transaction-already-reversed',
            detail: 'Transaction already reversed',
            params: {memberName: 'Jan Novák', amount: {amount: 250, currency: 'CZK'}},
        }));
        render(<ReverseConfirmModal isOpen transaction={transaction} onClose={vi.fn()}/>);

        await userEvent.click(screen.getByRole('button', {name: labels.finance.confirmReverse}));

        const message = await screen.findByText(/již byla stornována/);
        expect(message.textContent?.replace(/\s/g, ' '))
            .toBe('Transakce ve výši 250,00 Kč na účtu člena Jan Novák již byla stornována.');
    });

    it('shows the generic Czech text instead of the HTTP status for an untyped error', async () => {
        failWith(new ApiError(409));
        render(<ReverseConfirmModal isOpen transaction={transaction} onClose={vi.fn()}/>);

        await userEvent.click(screen.getByRole('button', {name: labels.finance.confirmReverse}));

        expect(await screen.findByText(/Akci nelze provést/)).toBeInTheDocument();
        expect(screen.queryByText(/HTTP/)).not.toBeInTheDocument();
    });
});
