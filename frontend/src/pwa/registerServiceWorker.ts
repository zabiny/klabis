let registrationPromise: Promise<ServiceWorkerRegistration | null> | null = null;

export function registerServiceWorker(): Promise<ServiceWorkerRegistration | null> {
    if (!('serviceWorker' in navigator)) {
        return Promise.resolve(null);
    }
    registrationPromise ??= navigator.serviceWorker.register(serviceWorkerUrl(), {
        scope: '/',
        updateViaCache: 'none',
    }).catch((error: unknown) => {
        registrationPromise = null;
        console.error('Service worker registration failed', error);
        return null;
    });
    return registrationPromise;
}

export function getServiceWorkerRegistration(): Promise<ServiceWorkerRegistration | null> {
    return registrationPromise ?? Promise.resolve(null);
}

function serviceWorkerUrl(): string {
    return import.meta.env.DEV ? '/dev-sw.js?dev-sw' : '/sw.js';
}
