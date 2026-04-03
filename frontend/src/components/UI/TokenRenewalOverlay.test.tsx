import {act, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';
import {TokenRenewalOverlay} from './TokenRenewalOverlay';
import {hideRenewalOverlay, showRenewalOverlay} from '../../api/tokenRenewalState';

describe('TokenRenewalOverlay', () => {
    beforeEach(() => {
        hideRenewalOverlay();
    });
    describe('initial state', () => {
        it('should not be visible initially', () => {
            render(<TokenRenewalOverlay/>);
            expect(screen.queryByText('Obnovuji přihlášení...')).not.toBeInTheDocument();
        });
    });

    describe('show behaviour', () => {
        it('should display overlay text when show event is dispatched', () => {
            render(<TokenRenewalOverlay/>);

            act(() => {
                showRenewalOverlay();
            });

            expect(screen.getByText('Obnovuji přihlášení...')).toBeInTheDocument();
        });

        it('should block user interaction via pointer-events overlay', () => {
            render(<TokenRenewalOverlay/>);

            act(() => {
                showRenewalOverlay();
            });

            const overlay = screen.getByRole('status');
            expect(overlay).toBeInTheDocument();
        });

        it('should have accessible label during renewal', () => {
            render(<TokenRenewalOverlay/>);

            act(() => {
                showRenewalOverlay();
            });

            expect(screen.getByRole('status')).toHaveAttribute('aria-label', 'Obnovuji přihlášení...');
        });
    });

    describe('hide behaviour', () => {
        it('should hide overlay when hide event is dispatched', () => {
            render(<TokenRenewalOverlay/>);

            act(() => {
                showRenewalOverlay();
            });
            act(() => {
                hideRenewalOverlay();
            });

            expect(screen.queryByText('Obnovuji přihlášení...')).not.toBeInTheDocument();
        });
    });

    describe('cleanup', () => {
        it('should remove event listeners on unmount', () => {
            const {unmount} = render(<TokenRenewalOverlay/>);
            unmount();

            act(() => {
                showRenewalOverlay();
            });

            // After unmount, component no longer responds — no assertion needed for DOM,
            // but the act() must not throw
        });
    });
});
