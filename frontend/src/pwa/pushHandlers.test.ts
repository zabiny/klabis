import {describe, expect, it, vi} from 'vitest';
import {handleNotificationClick, handlePush, type PushScope} from './pushHandlers';

function createScope(clients: unknown[] = []) {
    const showNotification = vi.fn().mockResolvedValue(undefined);
    const openWindow = vi.fn().mockResolvedValue(null);
    const scope = {
        location: {origin: 'https://klabis.test'},
        registration: {showNotification},
        clients: {matchAll: vi.fn().mockResolvedValue(clients), openWindow},
    } as unknown as PushScope;
    return {scope, showNotification, openWindow};
}

function pushEvent(data: unknown) {
    const waitUntil = vi.fn();
    const event = {
        data: data === undefined ? null : {json: () => (data instanceof Error ? (() => { throw data; })() : data)},
        waitUntil,
    };
    return {event: event as never, waitUntil};
}

function clickEvent(data: unknown) {
    const close = vi.fn();
    const waitUntil = vi.fn();
    return {event: {notification: {close, data}, waitUntil} as never, close, waitUntil};
}

function windowClient(url: string) {
    return {url, focus: vi.fn().mockResolvedValue(undefined), navigate: vi.fn().mockResolvedValue(null)};
}

describe('handlePush', () => {
    it('shows notification built from payload', async () => {
        const {scope, showNotification} = createScope();
        const {event, waitUntil} = pushEvent({title: 'T', body: 'B', url: '/events/1', tag: 'g'});

        handlePush(scope, event);
        await waitUntil.mock.calls[0][0];

        expect(showNotification).toHaveBeenCalledWith('T', {
            body: 'B',
            icon: '/pwa-192x192.png',
            badge: '/pwa-192x192.png',
            tag: 'g',
            data: {url: '/events/1'},
        });
    });

    it.each([
        ['missing data', undefined],
        ['invalid json', new Error('bad')],
        ['non-object payload', 'text'],
    ])('falls back to default title for %s', async (_name, data) => {
        const {scope, showNotification} = createScope();
        const {event, waitUntil} = pushEvent(data);

        handlePush(scope, event);
        await waitUntil.mock.calls[0][0];

        expect(showNotification).toHaveBeenCalledWith('Klabis', expect.objectContaining({data: {url: '/'}}));
    });

    it('defaults url to home when missing', async () => {
        const {scope, showNotification} = createScope();
        const {event, waitUntil} = pushEvent({title: 'T'});

        handlePush(scope, event);
        await waitUntil.mock.calls[0][0];

        expect(showNotification.mock.calls[0][1].data).toEqual({url: '/'});
    });
});

describe('handleNotificationClick', () => {
    it('closes notification and focuses and navigates existing same-origin client', async () => {
        const client = windowClient('https://klabis.test/members');
        const foreign = windowClient('https://other.test/');
        const {scope, openWindow} = createScope([foreign, client]);
        const {event, close, waitUntil} = clickEvent({url: '/events/1'});

        handleNotificationClick(scope, event);
        await waitUntil.mock.calls[0][0];

        expect(close).toHaveBeenCalled();
        expect(client.focus).toHaveBeenCalled();
        expect(client.navigate).toHaveBeenCalledWith('https://klabis.test/events/1');
        expect(foreign.focus).not.toHaveBeenCalled();
        expect(openWindow).not.toHaveBeenCalled();
    });

    it('opens new window when no same-origin client exists', async () => {
        const {scope, openWindow} = createScope([windowClient('https://other.test/')]);
        const {event, waitUntil} = clickEvent({url: '/events/1'});

        handleNotificationClick(scope, event);
        await waitUntil.mock.calls[0][0];

        expect(openWindow).toHaveBeenCalledWith('https://klabis.test/events/1');
    });

    it('opens home page when notification has no url', async () => {
        const {scope, openWindow} = createScope();
        const {event, waitUntil} = clickEvent(undefined);

        handleNotificationClick(scope, event);
        await waitUntil.mock.calls[0][0];

        expect(openWindow).toHaveBeenCalledWith('https://klabis.test/');
    });
});
