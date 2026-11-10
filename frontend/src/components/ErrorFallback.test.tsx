import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {vi} from 'vitest';
import ErrorFallback from './ErrorFallback';

describe('ErrorFallback', () => {
    it('shows only Czech text and logs the technical error to the console', () => {
        const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
        const error = new Error('Cannot read properties of undefined');

        render(<ErrorFallback error={error} resetErrorBoundary={vi.fn()}/>);

        expect(screen.getByText(/Aplikace narazila na neočekávanou chybu/)).toBeInTheDocument();
        expect(screen.queryByText(/Cannot read properties/)).not.toBeInTheDocument();
        expect(consoleError).toHaveBeenCalledWith('Unhandled application error', error);
        consoleError.mockRestore();
    });
});
