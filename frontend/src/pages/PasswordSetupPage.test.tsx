import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ApiError} from '../api/apiError';
import PasswordSetupPage from './PasswordSetupPage';

vi.mock('../api/passwordSetup', () => ({
    validateToken: vi.fn(),
    completePasswordSetup: vi.fn(),
}));

import {validateToken} from '../api/passwordSetup';

const renderPage = () =>
    render(
        <MemoryRouter initialEntries={['/password-setup?token=abc']}>
            <PasswordSetupPage/>
        </MemoryRouter>
    );

describe('PasswordSetupPage token validation', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it.each([
        ['/errors/token-expired', 410, 'Platnost odkazu vypršela. Požádejte o nový odkaz.'],
        ['/errors/token-already-used', 410, 'Tento odkaz již byl použit.'],
        ['/errors/invalid-token', 400, 'Odkaz pro nastavení hesla není platný.'],
    ])('shows Czech text for %s', async (type, status, expected) => {
        vi.mocked(validateToken).mockRejectedValue(new ApiError(status, {type, detail: 'English detail'}));

        renderPage();

        expect(await screen.findByText(expected)).toBeInTheDocument();
        expect(screen.queryByText(/English detail/)).not.toBeInTheDocument();
    });

    it('shows the generic text when the error is not an ApiError', async () => {
        vi.mocked(validateToken).mockRejectedValue(new TypeError('Failed to fetch'));

        renderPage();

        expect(await screen.findByText('Došlo k neočekávané chybě. Zkuste to prosím znovu.')).toBeInTheDocument();
    });
});
