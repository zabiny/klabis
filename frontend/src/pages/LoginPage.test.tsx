import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import LoginPage from './LoginPage';

vi.mock('../contexts/AuthContext2', () => ({
    useAuth: () => ({isAuthenticated: false, isLoading: false}),
}));

const renderLoginPage = (search: string = '') =>
    render(
        <MemoryRouter initialEntries={[`/login${search}`]}>
            <LoginPage/>
        </MemoryRouter>
    );

describe('LoginPage error handling', () => {
    it('shows no error when no error param present', () => {
        renderLoginPage();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('shows invalid credentials error when error param has no value', () => {
        renderLoginPage('?error');
        expect(screen.getByRole('alert')).toHaveTextContent('Nesprávné registrační číslo nebo heslo');
    });

    it('shows invalid credentials error when error param is empty string', () => {
        renderLoginPage('?error=');
        expect(screen.getByRole('alert')).toHaveTextContent('Nesprávné registrační číslo nebo heslo');
    });

    it('shows configuration error when error param has a specific OAuth2 value like invalid_client', () => {
        renderLoginPage('?error=invalid_client');
        expect(screen.getByRole('alert')).toHaveTextContent('Chyba konfigurace, kontaktujte administrátora');
    });

    it('shows configuration error for other OAuth2 error codes', () => {
        renderLoginPage('?error=invalid_redirect_uri');
        expect(screen.getByRole('alert')).toHaveTextContent('Chyba konfigurace, kontaktujte administrátora');
    });
});
