import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import ForbiddenPage from './ForbiddenPage';

const renderPage = () =>
    render(
        <MemoryRouter>
            <ForbiddenPage/>
        </MemoryRouter>
    );

describe('ForbiddenPage', () => {
    it('renders 403 heading', () => {
        renderPage();
        expect(screen.getByRole('heading', {level: 1, name: '403'})).toBeInTheDocument();
    });

    it('renders access denied title', () => {
        renderPage();
        expect(screen.getByRole('heading', {level: 2, name: /přístup odepřen/i})).toBeInTheDocument();
    });

    it('renders explanation text', () => {
        renderPage();
        expect(screen.getByText(/pro zobrazení této stránky nemáte potřebná oprávnění/i)).toBeInTheDocument();
    });

    it('renders a link back to home', () => {
        renderPage();
        const link = screen.getByRole('link', {name: /zpět na úvodní stránku/i});
        expect(link).toHaveAttribute('href', '/');
    });
});
