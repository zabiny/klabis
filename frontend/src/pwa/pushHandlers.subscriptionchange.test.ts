import {describe, expect, it, vi} from 'vitest';
import {handlePushSubscriptionChange, PUSH_SUBSCRIPTION_CHANGED, type PushScope} from './pushHandlers';

const KEY = new Uint8Array([1, 2, 3]).buffer;

function setup(opts: {clients?: {postMessage: ReturnType<typeof vi.fn>}[]; subscribe?: ReturnType<typeof vi.fn>} = {}) {
    const subscribe = opts.subscribe ?? vi.fn().mockResolvedValue({});
    const scope = {
        location: {origin: 'https://klabis.test'},
        registration: {pushManager: {subscribe}},
        clients: {matchAll: vi.fn().mockResolvedValue(opts.clients ?? []), openWindow: vi.fn()},
    } as unknown as PushScope;
    const waitUntil = vi.fn();
    const event = {oldSubscription: {options: {applicationServerKey: KEY}}, waitUntil};
    return {scope, subscribe, event: event as never, waitUntil};
}

describe('handlePushSubscriptionChange', () => {
    it('re-subscribes with the old application server key and notifies open clients', async () => {
        const client = {postMessage: vi.fn()};
        const {scope, subscribe, event, waitUntil} = setup({clients: [client]});

        handlePushSubscriptionChange(scope, event);
        await waitUntil.mock.calls[0][0];

        expect(subscribe).toHaveBeenCalledWith({userVisibleOnly: true, applicationServerKey: KEY});
        expect(client.postMessage).toHaveBeenCalledWith({type: PUSH_SUBSCRIPTION_CHANGED});
    });

    it('still notifies clients when re-subscribing fails', async () => {
        const client = {postMessage: vi.fn()};
        const {scope, event, waitUntil} = setup({clients: [client], subscribe: vi.fn().mockRejectedValue(new Error('x'))});

        handlePushSubscriptionChange(scope, event);
        await waitUntil.mock.calls[0][0];

        expect(client.postMessage).toHaveBeenCalledWith({type: PUSH_SUBSCRIPTION_CHANGED});
    });

    it('does nothing more when there is no open client', async () => {
        const {scope, event, waitUntil} = setup();

        handlePushSubscriptionChange(scope, event);

        await expect(waitUntil.mock.calls[0][0]).resolves.toBeUndefined();
    });
});
