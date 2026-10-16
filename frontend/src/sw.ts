/// <reference lib="webworker" />

const sw = self as unknown as ServiceWorkerGlobalScope;

const LEGACY_CACHE_NAMES = ['klabis-fonts'];
const LEGACY_CACHE_PREFIXES = ['workbox-'];

const isLegacyCache = (name: string) =>
    LEGACY_CACHE_NAMES.includes(name) || LEGACY_CACHE_PREFIXES.some((prefix) => name.startsWith(prefix));

sw.addEventListener('install', (event) => {
    event.waitUntil(sw.skipWaiting());
});

sw.addEventListener('activate', (event) => {
    event.waitUntil(activate());
});

async function activate(): Promise<void> {
    const removedLegacyCaches = await deleteLegacyCaches();
    await sw.clients.claim();
    if (removedLegacyCaches) {
        const windowClients = await sw.clients.matchAll({type: 'window'});
        for (const client of windowClients) {
            client.navigate(client.url);
        }
    }
}

async function deleteLegacyCaches(): Promise<boolean> {
    const cacheNames = await caches.keys();
    const legacyCacheNames = cacheNames.filter(isLegacyCache);
    await Promise.all(legacyCacheNames.map((name) => caches.delete(name)));
    return legacyCacheNames.length > 0;
}
