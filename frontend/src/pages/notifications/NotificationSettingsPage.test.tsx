import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import type {UseHalPageDataReturn} from '../../hooks/useHalPageData';
import {getServiceWorkerRegistration} from '../../pwa/registerServiceWorker';
import {labels} from '../../localization';
import {NotificationSettingsPage} from './NotificationSettingsPage';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));
vi.mock('../../pwa/registerServiceWorker', () => ({getServiceWorkerRegistration: vi.fn()}));

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

const renderPage = () => {
    vi.mocked(useHalPageData).mockReturnValue({
        resourceData: settings,
        isLoading: false,
        error: null,
    } as unknown as UseHalPageDataReturn);
    return render(<NotificationSettingsPage/>);
};

describe('NotificationSettingsPage', () => {
    beforeEach(() => {
        mockUserAgent('Mozilla/5.0 (X11; Linux x86_64) Chrome/120');
        mockSubscription(null);
        supportPush();
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
});
