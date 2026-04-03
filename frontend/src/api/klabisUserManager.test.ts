import {describe, it, expect, vi, beforeEach, afterEach} from 'vitest';
import type {UserManager} from 'oidc-client-ts';

// Bypass the global mock from setupTests.ts — this test exercises the real module
vi.unmock('./klabisUserManager');
const {silentRenewRetry} = await import('./klabisUserManager');

const MAX_ATTEMPTS = 3;
const RETRY_DELAY_MS = 5000;

/** Flush pending microtasks (resolved/rejected promises in the microtask queue). */
const flushMicrotasks = () => new Promise<void>((resolve) => queueMicrotask(resolve));

describe('silentRenewRetry', () => {
    let mockUserManager: {
        signinSilent: ReturnType<typeof vi.fn>;
        removeUser: ReturnType<typeof vi.fn>;
    };

    beforeEach(() => {
        vi.useFakeTimers();
        mockUserManager = {
            signinSilent: vi.fn(),
            removeUser: vi.fn().mockResolvedValue(undefined),
        };
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('resolves immediately when first signinSilent succeeds', async () => {
        mockUserManager.signinSilent.mockResolvedValue({access_token: 'new-token'});

        const promise = silentRenewRetry(mockUserManager as unknown as UserManager);
        await promise;

        expect(mockUserManager.signinSilent).toHaveBeenCalledTimes(1);
        expect(mockUserManager.removeUser).not.toHaveBeenCalled();
    });

    it('retries after 5s delay when first attempt fails', async () => {
        mockUserManager.signinSilent
            .mockRejectedValueOnce(new Error('renew failed'))
            .mockResolvedValue({access_token: 'new-token'});

        const promise = silentRenewRetry(mockUserManager as unknown as UserManager);

        // First attempt fires synchronously inside the loop, awaits the rejection
        await vi.advanceTimersByTimeAsync(0);
        expect(mockUserManager.signinSilent).toHaveBeenCalledTimes(1);

        // Advance past the retry delay — second attempt fires
        await vi.advanceTimersByTimeAsync(RETRY_DELAY_MS);
        await promise;

        expect(mockUserManager.signinSilent).toHaveBeenCalledTimes(2);
        expect(mockUserManager.removeUser).not.toHaveBeenCalled();
    });

    it('retries up to 3 total attempts before calling removeUser', async () => {
        mockUserManager.signinSilent.mockRejectedValue(new Error('renew failed'));

        const promise = silentRenewRetry(mockUserManager as unknown as UserManager);
        promise.catch(() => {});

        await vi.advanceTimersByTimeAsync(0);           // attempt 1 fails
        await vi.advanceTimersByTimeAsync(RETRY_DELAY_MS); // attempt 2 fails
        await vi.advanceTimersByTimeAsync(RETRY_DELAY_MS); // attempt 3 fails

        await expect(promise).rejects.toThrow();

        expect(mockUserManager.signinSilent).toHaveBeenCalledTimes(MAX_ATTEMPTS);
        expect(mockUserManager.removeUser).toHaveBeenCalledTimes(1);
    });

    it('does not start a second retry cycle when one is already in progress', async () => {
        mockUserManager.signinSilent.mockRejectedValue(new Error('renew failed'));

        const promise1 = silentRenewRetry(mockUserManager as unknown as UserManager);
        const promise2 = silentRenewRetry(mockUserManager as unknown as UserManager);
        // Suppress unhandled rejection warnings — both promises will be awaited below
        promise1.catch(() => {});
        promise2.catch(() => {});

        await vi.advanceTimersByTimeAsync(0);
        await vi.advanceTimersByTimeAsync(RETRY_DELAY_MS);
        await vi.advanceTimersByTimeAsync(RETRY_DELAY_MS);

        await expect(promise1).rejects.toThrow();
        await expect(promise2).rejects.toThrow();

        // Only 3 attempts total — no doubling from concurrent calls
        expect(mockUserManager.signinSilent).toHaveBeenCalledTimes(MAX_ATTEMPTS);
        expect(mockUserManager.removeUser).toHaveBeenCalledTimes(1);
    });

    it('allows a new retry cycle after the previous one completes', async () => {
        mockUserManager.signinSilent
            .mockRejectedValueOnce(new Error('fail'))
            .mockRejectedValueOnce(new Error('fail'))
            .mockRejectedValueOnce(new Error('fail'))
            // Second cycle succeeds immediately
            .mockResolvedValue({access_token: 'ok'});

        const promise1 = silentRenewRetry(mockUserManager as unknown as UserManager);
        promise1.catch(() => {});

        // Exhaust first cycle
        await vi.advanceTimersByTimeAsync(0);
        await vi.advanceTimersByTimeAsync(RETRY_DELAY_MS);
        await vi.advanceTimersByTimeAsync(RETRY_DELAY_MS);
        await expect(promise1).rejects.toThrow();

        // Flush the microtask queue so finally() clears activeRetryPromise
        await flushMicrotasks();
        await flushMicrotasks(); // double flush — removeUser() itself is async

        // Second cycle should start fresh
        const promise2 = silentRenewRetry(mockUserManager as unknown as UserManager);
        await promise2;

        expect(mockUserManager.signinSilent).toHaveBeenCalledTimes(4);
    });
});
