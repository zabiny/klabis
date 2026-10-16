import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

async function loadModule() {
    vi.resetModules();
    return await import('./registerServiceWorker');
}

describe('registerServiceWorker', () => {
    const registration = {scope: '/'} as unknown as ServiceWorkerRegistration;
    let register: ReturnType<typeof vi.fn>;
    let originalDescriptor: PropertyDescriptor | undefined;

    beforeEach(() => {
        register = vi.fn().mockResolvedValue(registration);
        originalDescriptor = Object.getOwnPropertyDescriptor(navigator, 'serviceWorker');
        Reflect.deleteProperty(navigator, 'serviceWorker');
    });

    afterEach(() => {
        if (originalDescriptor) {
            Object.defineProperty(navigator, 'serviceWorker', originalDescriptor);
        } else {
            Reflect.deleteProperty(navigator, 'serviceWorker');
        }
        vi.unstubAllEnvs();
        vi.restoreAllMocks();
    });

    function withServiceWorkerSupport() {
        Object.defineProperty(navigator, 'serviceWorker', {
            configurable: true,
            value: {register},
        });
    }

    it('skips registration when the service worker API is unsupported', async () => {
        const {registerServiceWorker, getServiceWorkerRegistration} = await loadModule();

        await expect(registerServiceWorker()).resolves.toBeNull();
        await expect(getServiceWorkerRegistration()).resolves.toBeNull();
        expect(register).not.toHaveBeenCalled();
    });

    it('registers /sw.js with root scope and no update cache in production', async () => {
        vi.stubEnv('DEV', false);
        withServiceWorkerSupport();
        const {registerServiceWorker, getServiceWorkerRegistration} = await loadModule();

        await expect(registerServiceWorker()).resolves.toBe(registration);
        expect(register).toHaveBeenCalledWith('/sw.js', {scope: '/', updateViaCache: 'none'});
        await expect(getServiceWorkerRegistration()).resolves.toBe(registration);
    });

    it('registers the dev worker in development', async () => {
        vi.stubEnv('DEV', true);
        withServiceWorkerSupport();
        const {registerServiceWorker} = await loadModule();

        await registerServiceWorker();
        expect(register).toHaveBeenCalledWith('/dev-sw.js?dev-sw', {scope: '/', updateViaCache: 'none'});
    });

    it('registers only once', async () => {
        vi.stubEnv('DEV', false);
        withServiceWorkerSupport();
        const {registerServiceWorker} = await loadModule();

        await registerServiceWorker();
        await registerServiceWorker();
        expect(register).toHaveBeenCalledOnce();
    });
});
