import React from 'react';
import {act, renderHook, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthProvider} from './AuthContext2';
import {useAuth} from './authContext';

const callOrder: string[] = [];
const signoutRedirect = vi.fn(async () => {
    callOrder.push('signoutRedirect');
});
const userManager = {
    getUser: vi.fn(async () => null),
    removeUser: vi.fn(async () => undefined),
    signoutRedirect,
    signinRedirectCallback: vi.fn(),
};

vi.mock('../api/klabisUserManager.ts', () => ({
    createUserManager: () => userManager,
}));

const getServiceWorkerRegistration = vi.fn();
vi.mock('../pwa/registerServiceWorker', () => ({
    getServiceWorkerRegistration: () => getServiceWorkerRegistration(),
}));

const clearRegisteredEndpoint = vi.fn(() => {
    callOrder.push('clearRegisteredEndpoint');
});
vi.mock('../pwa/pushSubscriptionStore', () => ({
    clearRegisteredEndpoint: () => clearRegisteredEndpoint(),
}));

const config = {
    authority: 'http://a',
    client_id: 'c',
    redirect_uri: 'http://localhost/callback',
} as never;

const wrapper = ({children}: { children: React.ReactNode }) => (
    <MemoryRouter>
        <AuthProvider config={config}>{children}</AuthProvider>
    </MemoryRouter>
);

async function renderAuth() {
    const hook = renderHook(() => useAuth(), {wrapper});
    await waitFor(() => expect(hook.result.current.isLoading).toBe(false));
    return hook;
}

function registrationWith(getSubscription: () => Promise<unknown>) {
    return {pushManager: {getSubscription}};
}

describe('AuthProvider logout', () => {
    beforeEach(() => {
        callOrder.length = 0;
        vi.clearAllMocks();
        vi.stubGlobal('PushManager', class {});
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.unstubAllGlobals();
    });

    it('unsubscribes the push subscription before signoutRedirect', async () => {
        const unsubscribe = vi.fn(async () => {
            callOrder.push('unsubscribe');
            return true;
        });
        getServiceWorkerRegistration.mockResolvedValue(registrationWith(async () => ({unsubscribe})));
        const {result} = await renderAuth();

        await act(() => result.current.logout());

        expect(callOrder).toEqual(['clearRegisteredEndpoint', 'unsubscribe', 'signoutRedirect']);
    });

    it('logs out when there is no subscription', async () => {
        getServiceWorkerRegistration.mockResolvedValue(registrationWith(async () => null));
        const {result} = await renderAuth();

        await act(() => result.current.logout());

        expect(clearRegisteredEndpoint).toHaveBeenCalled();
        expect(signoutRedirect).toHaveBeenCalled();
    });

    it('logs out when there is no service worker registration', async () => {
        getServiceWorkerRegistration.mockResolvedValue(null);
        const {result} = await renderAuth();

        await act(() => result.current.logout());

        expect(signoutRedirect).toHaveBeenCalled();
    });

    it('logs out when unsubscribe rejects', async () => {
        getServiceWorkerRegistration.mockResolvedValue(
            registrationWith(async () => ({unsubscribe: () => Promise.reject(new Error('boom'))})));
        const {result} = await renderAuth();

        await act(() => result.current.logout());

        expect(clearRegisteredEndpoint).toHaveBeenCalled();
        expect(signoutRedirect).toHaveBeenCalled();
    });

    it('logs out when push is unsupported', async () => {
        vi.stubGlobal('PushManager', undefined);
        delete (globalThis as {PushManager?: unknown}).PushManager;
        const {result} = await renderAuth();

        await act(() => result.current.logout());

        expect(getServiceWorkerRegistration).not.toHaveBeenCalled();
        expect(signoutRedirect).toHaveBeenCalled();
    });

    it('logs out when unsubscribe hangs', async () => {
        getServiceWorkerRegistration.mockResolvedValue(
            registrationWith(async () => ({unsubscribe: () => new Promise(() => {})})));
        const {result} = await renderAuth();
        vi.useFakeTimers();

        const logout = act(() => result.current.logout());
        await vi.advanceTimersByTimeAsync(2000);
        await logout;

        expect(signoutRedirect).toHaveBeenCalled();
        expect(clearRegisteredEndpoint).toHaveBeenCalled();
    });
});
