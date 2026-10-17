import React from 'react';
import {act, renderHook, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthProvider} from './AuthContext2';
import {useAuth} from './authContext';
import type {User, UserManager} from 'oidc-client-ts';

const callOrder: string[] = [];
const signoutRedirect = vi.fn(async () => {
    callOrder.push('signoutRedirect');
});
const userManager = {
    getUser: vi.fn(async () => null),
    signinRedirect: vi.fn(async () => undefined),
    signoutRedirect,
    signinRedirectCallback: vi.fn(),
    settings: {redirect_uri: 'http://localhost/callback'},
    events: {
        addUserLoaded: vi.fn(),
        addUserUnloaded: vi.fn(),
        removeUserLoaded: vi.fn(),
        removeUserUnloaded: vi.fn(),
    },
} as unknown as UserManager;

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

const wrapper = ({children}: { children: React.ReactNode }) => (
    <MemoryRouter>
        <AuthProvider userManager={userManager}>{children}</AuthProvider>
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

describe('AuthProvider user state', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('follows user loaded and unloaded events of the shared user manager', async () => {
        const {result} = await renderAuth();
        const onUserLoaded = vi.mocked(userManager.events.addUserLoaded).mock.calls.at(-1)![0];
        const onUserUnloaded = vi.mocked(userManager.events.addUserUnloaded).mock.calls.at(-1)![0];

        act(() => onUserLoaded({expired: false, profile: {sub: '1'}} as User));
        expect(result.current.isAuthenticated).toBe(true);

        act(() => onUserUnloaded());
        expect(result.current.isAuthenticated).toBe(false);
    });
});

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

    it('does not start sign-in while logout is in progress', async () => {
        getServiceWorkerRegistration.mockResolvedValue(
            registrationWith(async () => ({unsubscribe: () => new Promise(() => {})})));
        const {result} = await renderAuth();
        vi.useFakeTimers();

        const logout = act(() => result.current.logout());
        act(() => result.current.login());
        await vi.advanceTimersByTimeAsync(2000);
        await logout;
        act(() => result.current.login());

        expect(userManager.signinRedirect).not.toHaveBeenCalled();
        expect(signoutRedirect).toHaveBeenCalledTimes(1);
    });

    it('allows sign-in again after signoutRedirect rejects', async () => {
        getServiceWorkerRegistration.mockResolvedValue(registrationWith(async () => null));
        signoutRedirect.mockRejectedValueOnce(new Error('boom'));
        const {result} = await renderAuth();

        await act(() => result.current.logout());
        act(() => result.current.login());

        expect(userManager.signinRedirect).toHaveBeenCalledTimes(1);
    });
});
