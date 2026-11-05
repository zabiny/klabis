import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {ErrorPage} from './ErrorPage';
import {ApiError} from '../api/apiError';

vi.mock('./NotFoundPage', () => ({
    default: () => <div data-testid="not-found-page">404 Not Found</div>,
}));

vi.mock('./ForbiddenPage', () => ({
    default: () => <div data-testid="forbidden-page">403 Forbidden</div>,
}));

const renderPage = (error: Parameters<typeof ErrorPage>[0]['error']) =>
    render(
        <MemoryRouter>
            <ErrorPage error={error}/>
        </MemoryRouter>
    );

describe('ErrorPage', () => {
    describe('status detection via ApiError status', () => {
        it('renders ForbiddenPage for 403 error', () => {
            const error = new ApiError(403);
            renderPage(error);
            expect(screen.getByTestId('forbidden-page')).toBeInTheDocument();
        });

        it('renders ForbiddenPage for 401 error', () => {
            const error = new ApiError(401);
            renderPage(error);
            expect(screen.getByTestId('forbidden-page')).toBeInTheDocument();
        });

        it('renders NotFoundPage for 404 error', () => {
            const error = new ApiError(404);
            renderPage(error);
            expect(screen.getByTestId('not-found-page')).toBeInTheDocument();
        });

        it('renders generic error page for 500 error', () => {
            renderPage(new ApiError(500));
            expect(screen.getByText(/něco se pokazilo/i)).toBeInTheDocument();
            expect(screen.getByText('Došlo k neočekávané chybě. Zkuste to prosím znovu.')).toBeInTheDocument();
        });
    });

    describe('plain errors', () => {
        it('does not guess a status from the message', () => {
            renderPage(new Error('HTTP 404 Not Found'));
            expect(screen.queryByTestId('not-found-page')).not.toBeInTheDocument();
            expect(screen.getByText('Došlo k neočekávané chybě. Zkuste to prosím znovu.')).toBeInTheDocument();
        });

        it('renders a status given as a plain object', () => {
            renderPage({status: 403});
            expect(screen.getByTestId('forbidden-page')).toBeInTheDocument();
        });

        it('renders generic error page for unknown error message', () => {
            renderPage(new Error('Network timeout'));
            expect(screen.getByText(/něco se pokazilo/i)).toBeInTheDocument();
            expect(screen.getByText('Došlo k neočekávané chybě. Zkuste to prosím znovu.')).toBeInTheDocument();
            expect(screen.queryByText('Network timeout')).not.toBeInTheDocument();
        });

        it('renders NotFoundPage for a typed error with status 404', () => {
            renderPage(new ApiError(404, {type: '/errors/event-not-found'}));
            expect(screen.getByTestId('not-found-page')).toBeInTheDocument();
        });

        it('renders the Czech text of a typed error', () => {
            renderPage(new ApiError(409, {type: '/errors/finished-event-cannot-be-updated'}));
            expect(screen.getByText('Ukončenou akci nelze upravovat.')).toBeInTheDocument();
        });
    });

    describe('null/undefined error', () => {
        it('renders nothing for null error', () => {
            const {container} = renderPage(null);
            expect(container).toBeEmptyDOMElement();
        });

        it('renders nothing for undefined error', () => {
            const {container} = renderPage(undefined);
            expect(container).toBeEmptyDOMElement();
        });
    });
});
