import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import {NegativeBalanceSuspensionDialog} from './NegativeBalanceSuspensionDialog';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
    const actual = await importOriginal<typeof import('react-router-dom')>();
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

const baseWarning = {
    balance: {amount: -250, currency: 'CZK'},
    accountLink: '/api/members/abc-123/account',
};

const renderDialog = (props?: Partial<{isOpen: boolean; warning: typeof baseWarning | null}>) => {
    const mergedProps = {
        isOpen: true,
        onClose: vi.fn(),
        warning: baseWarning,
        ...props,
    };
    return render(
        <MemoryRouter>
            <NegativeBalanceSuspensionDialog {...mergedProps}/>
        </MemoryRouter>
    );
};

describe('NegativeBalanceSuspensionDialog (task 9.1)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders dialog title when open', () => {
        renderDialog();
        expect(screen.getByText('Záporný zůstatek')).toBeInTheDocument();
    });

    it('renders description text explaining balance must be non-negative', () => {
        renderDialog();
        expect(screen.getByText(/Pro ukončení členství musí být zůstatek nezáporný/i)).toBeInTheDocument();
    });

    it('renders current balance label', () => {
        renderDialog();
        expect(screen.getByText(/Aktuální zůstatek/i)).toBeInTheDocument();
    });

    it('renders formatted balance amount', () => {
        renderDialog();
        expect(screen.getByText(/-250,00 CZK/)).toBeInTheDocument();
    });

    it('renders "Otevřít účet" button', () => {
        renderDialog();
        expect(screen.getByRole('button', {name: /Otevřít účet/i})).toBeInTheDocument();
    });

    it('renders "Zavřít" button', () => {
        renderDialog();
        expect(screen.getByRole('button', {name: /Zavřít/i})).toBeInTheDocument();
    });

    it('calls onClose when "Zavřít" is clicked', () => {
        const onClose = vi.fn();
        render(
            <MemoryRouter>
                <NegativeBalanceSuspensionDialog isOpen={true} onClose={onClose} warning={baseWarning}/>
            </MemoryRouter>
        );
        fireEvent.click(screen.getByRole('button', {name: /Zavřít/i}));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('navigates to member account page and closes dialog when "Otevřít účet" is clicked', () => {
        const onClose = vi.fn();
        render(
            <MemoryRouter>
                <NegativeBalanceSuspensionDialog isOpen={true} onClose={onClose} warning={baseWarning}/>
            </MemoryRouter>
        );
        fireEvent.click(screen.getByRole('button', {name: /Otevřít účet/i}));
        expect(onClose).toHaveBeenCalledTimes(1);
        // extractNavigationPath strips /api prefix for React Router
        expect(mockNavigate).toHaveBeenCalledWith('/members/abc-123/account');
    });

    it('renders nothing when isOpen is false', () => {
        renderDialog({isOpen: false});
        expect(screen.queryByText('Záporný zůstatek')).not.toBeInTheDocument();
    });

    it('renders nothing when warning is null', () => {
        renderDialog({warning: null});
        expect(screen.queryByText('Záporný zůstatek')).not.toBeInTheDocument();
    });

    it('does NOT render any override or force-suspend toggle', () => {
        renderDialog();
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
        expect(screen.queryByText(/přepsat/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/potvrdit ukončení/i)).not.toBeInTheDocument();
    });
});

describe('parseNegativeBalanceWarning409', () => {
    it('parses negative-balance 409 body into NegativeBalanceWarning', async () => {
        const {parseNegativeBalanceWarning409} = await import('./suspensionUtils');
        const {FetchError} = await import('../../api/authorizedFetch');

        const error = new FetchError(
            '409',
            409,
            'Conflict',
            new Headers(),
            JSON.stringify({balance: {amount: -250, currency: 'CZK'}, accountLink: '/api/members/abc/account'}),
        );

        const result = parseNegativeBalanceWarning409(error);
        expect(result).not.toBeNull();
        expect(result?.balance.amount).toBe(-250);
        expect(result?.balance.currency).toBe('CZK');
        expect(result?.accountLink).toBe('/api/members/abc/account');
    });

    it('returns null for non-409 error', async () => {
        const {parseNegativeBalanceWarning409} = await import('./suspensionUtils');
        const {FetchError} = await import('../../api/authorizedFetch');

        const error = new FetchError('500', 500, 'Internal Server Error', new Headers(), '{}');
        expect(parseNegativeBalanceWarning409(error)).toBeNull();
    });

    it('returns null for 409 with LastOwner body shape (affectedGroups)', async () => {
        const {parseNegativeBalanceWarning409} = await import('./suspensionUtils');
        const {FetchError} = await import('../../api/authorizedFetch');

        const error = new FetchError(
            '409',
            409,
            'Conflict',
            new Headers(),
            JSON.stringify({message: 'last owner', affectedGroups: [{groupId: 'g1', groupName: 'Group', groupType: 'FREE'}]}),
        );

        expect(parseNegativeBalanceWarning409(error)).toBeNull();
    });

    it('returns null for non-FetchError', async () => {
        const {parseNegativeBalanceWarning409} = await import('./suspensionUtils');
        expect(parseNegativeBalanceWarning409(new Error('generic'))).toBeNull();
    });
});
