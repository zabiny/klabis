import '@testing-library/jest-dom';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {useHalPageData} from '../../hooks/useHalPageData';
import type {UseHalPageDataReturn} from '../../hooks/useHalPageData';
import {getServiceWorkerRegistration} from '../../pwa/registerServiceWorker';
import {labels} from '../../localization';
import {NotificationSettingsPage} from './NotificationSettingsPage';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));
vi.mock('../../pwa/registerServiceWorker', () => ({getServiceWorkerRegistration: vi.fn()}));
vi.mock('../../hooks/useAuthorizedFetch', () => ({useAuthorizedMutation: vi.fn()}));

const refetch = vi.fn();
const mutate = vi.fn();

const CURRENT_ENDPOINT = 'https://push.example/current';

const settings = {
    vapidPublicKey: 'key',
    devices: [
        {id: '1', description: 'Chrome · Android', enabledAt: '2026-10-01T10:00:00Z', endpoint: CURRENT_ENDPOINT},
        {id: '2', description: 'Firefox · Linux', enabledAt: '2026-10-02T10:00:00Z', endpoint: 'https://push.example/other'},
    ],
};

const original = {
    notification: Object.getOwnPropertyDescriptor(window, 'Notification'),
    pushManager: Object.getOwnPropertyDescriptor(window, 'PushManager'),
    userAgent: Object.getOwnPropertyDescriptor(window.navigator, 'userAgent'),
    matchMedia: window.matchMedia,
};

const setWindowProp = (name: string, value: unknown) =>
    Object.defineProperty(window, name, {value, configurable: true, writable: true});

const supportPush = (permission: NotificationPermission = 'default') => {
    setWindowProp('PushManager', class {});
    setWindowProp('Notification', {permission, requestPermission: vi.fn()});
};

const mockSubscription = (endpoint: string | null) => {
    vi.mocked(getServiceWorkerRegistration).mockResolvedValue({
        pushManager: {getSubscription: vi.fn().mockResolvedValue(endpoint ? {endpoint} : null)},
    } as unknown as ServiceWorkerRegistration);
};

const mockUserAgent = (ua: string, standalone = false) => {
    Object.defineProperty(window.navigator, 'userAgent', {value: ua, configurable: true});
    window.matchMedia = vi.fn().mockReturnValue({matches: standalone}) as unknown as typeof window.matchMedia;
};

const TEMPLATE_TARGET = '/api/notifications/devices';

const withTemplate = {
    ...settings,
    _templates: {registerNotificationDevice: {method: 'POST', target: TEMPLATE_TARGET, properties: []}},
};

const renderPage = (resourceData: unknown = settings) => {
    vi.mocked(useHalPageData).mockReturnValue({
        resourceData,
        isLoading: false,
        error: null,
        route: {refetch},
    } as unknown as UseHalPageDataReturn);
    return render(<NotificationSettingsPage/>);
};

describe('NotificationSettingsPage', () => {
    beforeEach(() => {
        mockUserAgent('Mozilla/5.0 (X11; Linux x86_64) Chrome/120');
        mockSubscription(null);
        supportPush();
        mutate.mockImplementation((_vars, options) => options?.onSuccess?.());
        vi.mocked(useAuthorizedMutation).mockReturnValue({mutate, isPending: false} as never);
    });

    afterEach(() => {
        const restore = (name: string, d?: PropertyDescriptor) =>
            d ? Object.defineProperty(window, name, d) : Reflect.deleteProperty(window, name);
        restore('Notification', original.notification);
        restore('PushManager', original.pushManager);
        if (original.userAgent) Object.defineProperty(window.navigator, 'userAgent', original.userAgent);
        window.matchMedia = original.matchMedia;
        vi.clearAllMocks();
    });

    it('shows unsupported message when browser lacks push support', async () => {
        Reflect.deleteProperty(window, 'PushManager');
        renderPage();
        expect(await screen.findByText(labels.notifications.stateUnsupported)).toBeInTheDocument();
    });

    it('asks iOS user to install the app when not running standalone', async () => {
        mockUserAgent('Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Safari', false);
        renderPage();
        expect(await screen.findByText(labels.notifications.stateIosNeedsInstall)).toBeInTheDocument();
    });

    it('does not show install hint on iOS in standalone mode', async () => {
        mockUserAgent('Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Safari', true);
        renderPage();
        expect(await screen.findByText(labels.notifications.stateOff)).toBeInTheDocument();
        expect(screen.queryByText(labels.notifications.stateIosNeedsInstall)).not.toBeInTheDocument();
    });

    it('explains how to allow notifications when permission is denied', async () => {
        supportPush('denied');
        renderPage();
        expect(await screen.findByText(labels.notifications.stateDenied)).toBeInTheDocument();
    });

    it('shows off state when there is no push subscription', async () => {
        renderPage();
        expect(await screen.findByText(labels.notifications.stateOff)).toBeInTheDocument();
    });

    it('shows off state when subscription endpoint is not among devices', async () => {
        mockSubscription('https://push.example/unknown');
        renderPage();
        expect(await screen.findByText(labels.notifications.stateOff)).toBeInTheDocument();
    });

    it('shows on state and marks current device when endpoint matches', async () => {
        mockSubscription(CURRENT_ENDPOINT);
        renderPage();
        expect(await screen.findByText(labels.notifications.stateOn)).toBeInTheDocument();
        expect(screen.getAllByText(labels.notifications.currentDevice)).toHaveLength(1);
        expect(screen.getByText('Chrome · Android')).toBeInTheDocument();
        expect(screen.getByText('Firefox · Linux')).toBeInTheDocument();
    });

    it('never requests permission on load', async () => {
        renderPage();
        await waitFor(() => expect(screen.getByText(labels.notifications.stateOff)).toBeInTheDocument());
        expect(window.Notification.requestPermission).not.toHaveBeenCalled();
    });

    describe('enabling notifications on this device', () => {
        const enableLabel = labels.notifications.enableOnThisDevice;
        const subscribe = vi.fn();
        const subscription = {
            endpoint: 'https://push.example/new',
            toJSON: () => ({endpoint: 'https://push.example/new', keys: {p256dh: 'p256dh-key', auth: 'auth-key'}}),
        };

        beforeEach(() => {
            subscribe.mockResolvedValue(subscription);
            vi.mocked(getServiceWorkerRegistration).mockResolvedValue({
                pushManager: {getSubscription: vi.fn().mockResolvedValue(null), subscribe},
            } as unknown as ServiceWorkerRegistration);
        });

        it('shows the enable button in off state when template is present', async () => {
            renderPage(withTemplate);
            expect(await screen.findByRole('button', {name: enableLabel})).toBeInTheDocument();
        });

        it('hides the enable button when template is missing', async () => {
            renderPage(settings);
            await screen.findByText(labels.notifications.stateOff);
            expect(screen.queryByRole('button', {name: enableLabel})).not.toBeInTheDocument();
        });

        it('hides the enable button in on state', async () => {
            mockSubscription(CURRENT_ENDPOINT);
            renderPage(withTemplate);
            await screen.findByText(labels.notifications.stateOn);
            expect(screen.queryByRole('button', {name: enableLabel})).not.toBeInTheDocument();
        });

        it('hides the enable button when permission is denied', async () => {
            supportPush('denied');
            renderPage(withTemplate);
            await screen.findByText(labels.notifications.stateDenied);
            expect(screen.queryByRole('button', {name: enableLabel})).not.toBeInTheDocument();
        });

        it('does not request permission on load', async () => {
            renderPage(withTemplate);
            await screen.findByRole('button', {name: enableLabel});
            expect(window.Notification.requestPermission).not.toHaveBeenCalled();
        });

        it('shows denied explanation when user refuses the permission', async () => {
            vi.mocked(window.Notification.requestPermission).mockResolvedValue('denied');
            renderPage(withTemplate);
            fireEvent.click(await screen.findByRole('button', {name: enableLabel}));
            expect(await screen.findByText(labels.notifications.stateDenied)).toBeInTheDocument();
            expect(subscribe).not.toHaveBeenCalled();
            expect(mutate).not.toHaveBeenCalled();
        });

        it('subscribes with the VAPID key and submits the template', async () => {
            vi.mocked(window.Notification.requestPermission).mockResolvedValue('granted');
            renderPage({...withTemplate, vapidPublicKey: 'AQID-_'});
            fireEvent.click(await screen.findByRole('button', {name: enableLabel}));

            await waitFor(() => expect(mutate).toHaveBeenCalled());
            const options = subscribe.mock.calls[0][0];
            expect(options.userVisibleOnly).toBe(true);
            expect(Array.from(new Uint8Array(options.applicationServerKey))).toEqual([1, 2, 3, 251]);
            expect(mutate.mock.calls[0][0]).toEqual({
                url: TEMPLATE_TARGET,
                data: {endpoint: 'https://push.example/new', p256dh: 'p256dh-key', auth: 'auth-key'},
            });
            await waitFor(() => expect(refetch).toHaveBeenCalled());
        });

        it('shows an error when subscribing fails', async () => {
            vi.mocked(window.Notification.requestPermission).mockResolvedValue('granted');
            subscribe.mockRejectedValue(new Error('boom'));
            renderPage(withTemplate);
            fireEvent.click(await screen.findByRole('button', {name: enableLabel}));
            expect(await screen.findByText(labels.notifications.enableFailed)).toBeInTheDocument();
        });

        it('shows an error when the backend rejects the registration', async () => {
            vi.mocked(window.Notification.requestPermission).mockResolvedValue('granted');
            mutate.mockImplementation((_vars, options) => options?.onError?.(new Error('500')));
            renderPage(withTemplate);
            fireEvent.click(await screen.findByRole('button', {name: enableLabel}));
            expect(await screen.findByText(labels.notifications.enableFailed)).toBeInTheDocument();
        });
    });
    describe('test notification', () => {
        const TEST_TARGET = '/api/notifications/test';
        const testLabel = labels.notifications.sendTest;
        const withTestTemplate = {
            ...settings,
            _templates: {sendTestNotification: {method: 'POST', target: TEST_TARGET, properties: []}},
        };

        it('hides the button without the template', async () => {
            renderPage();
            await screen.findByText(labels.notifications.stateOff);
            expect(screen.queryByRole('button', {name: testLabel})).not.toBeInTheDocument();
        });

        it('submits the template and shows success', async () => {
            renderPage(withTestTemplate);
            fireEvent.click(await screen.findByRole('button', {name: testLabel}));

            expect(mutate.mock.calls[0][0]).toEqual({url: TEST_TARGET});
            expect(await screen.findByText(labels.notifications.sendTestSuccess)).toBeInTheDocument();
        });

        it('shows an error when sending fails', async () => {
            mutate.mockImplementation((_vars, options) => options?.onError?.(new Error('500')));
            renderPage(withTestTemplate);
            fireEvent.click(await screen.findByRole('button', {name: testLabel}));

            expect(await screen.findByText(labels.notifications.sendTestFailed)).toBeInTheDocument();
            expect(screen.queryByText(labels.notifications.sendTestSuccess)).not.toBeInTheDocument();
        });
    });
});
