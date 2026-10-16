import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import type {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {authorizedFetch} from '../api/authorizedFetch';
import {useAuthorizedQuery} from '../hooks/useAuthorizedFetch';
import {getServiceWorkerRegistration} from './registerServiceWorker';
import {getRegisteredEndpoint, setRegisteredEndpoint} from './pushSubscriptionStore';
import {PUSH_SUBSCRIPTION_CHANGED} from './pushHandlers';
import {usePushReregistration} from './usePushReregistration';

vi.mock('../api/authorizedFetch', () => ({authorizedFetch: vi.fn()}));
vi.mock('../hooks/useAuthorizedFetch', () => ({useAuthorizedQuery: vi.fn()}));
vi.mock('./registerServiceWorker', () => ({getServiceWorkerRegistration: vi.fn()}));

const OLD = 'https://push.example/old';
const NEW = 'https://push.example/new';
const SETTINGS = '/api/notifications';
const TARGET = '/api/notifications/devices';

const setPermission = (permission: NotificationPermission | undefined) =>
    Object.defineProperty(window, 'Notification', {
        value: permission ? {permission} : undefined, configurable: true, writable: true,
    });

const subscribeAs = (endpoint: string | null) =>
    vi.mocked(getServiceWorkerRegistration).mockResolvedValue({
        pushManager: {
            getSubscription: vi.fn().mockResolvedValue(endpoint ? {
                endpoint,
                toJSON: () => ({keys: {p256dh: 'p', auth: 'a'}}),
            } : null),
        },
    } as unknown as ServiceWorkerRegistration);

const settingsResponse = (devices: {endpoint: string}[]) =>
    ({json: async () => ({devices, _templates: {registerNotificationDevice: {target: TARGET}}})}) as Response;

const wrapper = ({children}: {children: ReactNode}) => (
    <QueryClientProvider client={new QueryClient()}>{children}</QueryClientProvider>
);

describe('usePushReregistration', () => {
    beforeEach(() => {
        localStorage.clear();
        vi.mocked(authorizedFetch).mockReset();
        vi.mocked(useAuthorizedQuery).mockReturnValue({data: SETTINGS} as never);
        setPermission('granted');
    });
    afterEach(() => vi.clearAllMocks());

    it('re-registers when current endpoint differs from the remembered one', async () => {
        setRegisteredEndpoint(OLD);
        subscribeAs(NEW);
        vi.mocked(authorizedFetch).mockResolvedValueOnce(settingsResponse([{endpoint: OLD}])).mockResolvedValueOnce({} as Response);

        renderHook(() => usePushReregistration(), {wrapper});

        await waitFor(() => expect(authorizedFetch).toHaveBeenCalledTimes(2));
        const [url, init] = vi.mocked(authorizedFetch).mock.calls[1];
        expect(url).toBe(TARGET);
        expect(init?.method).toBe('POST');
        expect(JSON.parse(init?.body as string)).toEqual({endpoint: NEW, p256dh: 'p', auth: 'a'});
        await waitFor(() => expect(getRegisteredEndpoint()).toBe(NEW));
    });

    it('does nothing when notifications were never enabled on this device', async () => {
        subscribeAs(NEW);
        renderHook(() => usePushReregistration(), {wrapper});
        await new Promise(r => setTimeout(r, 10));
        expect(authorizedFetch).not.toHaveBeenCalled();
    });

    it('does nothing when the endpoint is unchanged', async () => {
        setRegisteredEndpoint(OLD);
        subscribeAs(OLD);
        renderHook(() => usePushReregistration(), {wrapper});
        await new Promise(r => setTimeout(r, 10));
        expect(authorizedFetch).not.toHaveBeenCalled();
    });

    it('does nothing when permission is not granted', async () => {
        setPermission('default');
        setRegisteredEndpoint(OLD);
        subscribeAs(NEW);
        renderHook(() => usePushReregistration(), {wrapper});
        await new Promise(r => setTimeout(r, 10));
        expect(authorizedFetch).not.toHaveBeenCalled();
    });

    it('only remembers the endpoint when the server already knows it', async () => {
        setRegisteredEndpoint(OLD);
        subscribeAs(NEW);
        vi.mocked(authorizedFetch).mockResolvedValueOnce(settingsResponse([{endpoint: NEW}]));

        renderHook(() => usePushReregistration(), {wrapper});

        await waitFor(() => expect(getRegisteredEndpoint()).toBe(NEW));
        expect(authorizedFetch).toHaveBeenCalledTimes(1);
    });

    it('checks again when the service worker reports a subscription change', async () => {
        setRegisteredEndpoint(OLD);
        subscribeAs(OLD);
        const listeners: ((e: MessageEvent) => void)[] = [];
        Object.defineProperty(navigator, 'serviceWorker', {
            configurable: true,
            value: {
                addEventListener: (_: string, l: (e: MessageEvent) => void) => listeners.push(l),
                removeEventListener: vi.fn(),
            },
        });
        renderHook(() => usePushReregistration(), {wrapper});
        await new Promise(r => setTimeout(r, 10));
        expect(authorizedFetch).not.toHaveBeenCalled();

        subscribeAs(NEW);
        vi.mocked(authorizedFetch).mockResolvedValueOnce(settingsResponse([])).mockResolvedValueOnce({} as Response);
        listeners.forEach(l => l({data: {type: PUSH_SUBSCRIPTION_CHANGED}} as MessageEvent));

        await waitFor(() => expect(authorizedFetch).toHaveBeenCalledTimes(2));
    });
});
