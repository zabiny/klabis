import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {hideRenewalOverlay, showRenewalOverlay} from './tokenRenewalState';

describe('tokenRenewalState', () => {
    beforeEach(() => {
        hideRenewalOverlay();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('showRenewalOverlay', () => {
        it('should dispatch show event on window', () => {
            const listener = vi.fn();
            window.addEventListener('klabis:renewal:show', listener);

            showRenewalOverlay();

            expect(listener).toHaveBeenCalledOnce();
            window.removeEventListener('klabis:renewal:show', listener);
        });

        it('should not dispatch event again when already shown (idempotent)', () => {
            const listener = vi.fn();
            window.addEventListener('klabis:renewal:show', listener);

            showRenewalOverlay();
            showRenewalOverlay();

            expect(listener).toHaveBeenCalledOnce();
            window.removeEventListener('klabis:renewal:show', listener);
        });
    });

    describe('hideRenewalOverlay', () => {
        it('should dispatch hide event on window', () => {
            showRenewalOverlay();

            const listener = vi.fn();
            window.addEventListener('klabis:renewal:hide', listener);

            hideRenewalOverlay();

            expect(listener).toHaveBeenCalledOnce();
            window.removeEventListener('klabis:renewal:hide', listener);
        });

        it('should not dispatch event when already hidden (idempotent)', () => {
            const listener = vi.fn();
            window.addEventListener('klabis:renewal:hide', listener);

            hideRenewalOverlay();

            expect(listener).not.toHaveBeenCalled();
            window.removeEventListener('klabis:renewal:hide', listener);
        });

        it('should allow showing overlay again after hiding', () => {
            const showListener = vi.fn();
            window.addEventListener('klabis:renewal:show', showListener);

            showRenewalOverlay();
            hideRenewalOverlay();
            showRenewalOverlay();

            expect(showListener).toHaveBeenCalledTimes(2);
            window.removeEventListener('klabis:renewal:show', showListener);
        });
    });
});
