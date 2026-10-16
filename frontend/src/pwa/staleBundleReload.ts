const HIDDEN_THRESHOLD_MS = 15 * 60 * 1000;
const INDEX_HTML_URL = '/index.html';

export function installStaleBundleReload(reload: () => void = () => location.reload()): void {
    let hiddenAt: number | null = null;

    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'hidden') {
            hiddenAt = Date.now();
            return;
        }
        if (document.visibilityState !== 'visible' || hiddenAt === null) {
            return;
        }
        const hiddenForMs = Date.now() - hiddenAt;
        hiddenAt = null;
        if (hiddenForMs >= HIDDEN_THRESHOLD_MS) {
            void reloadIfStale(reload);
        }
    });
}

async function reloadIfStale(reload: () => void): Promise<void> {
    try {
        const response = await fetch(INDEX_HTML_URL, {cache: 'no-store'});
        if (!response.ok) {
            return;
        }
        const servedBundle = extractBundleUrl(await response.text());
        const currentBundle = currentBundleUrl();
        if (servedBundle && currentBundle && servedBundle !== currentBundle) {
            reload();
        }
    } catch {
        // Keep the running bundle when the check cannot be performed.
    }
}

function extractBundleUrl(html: string): string | null {
    const document = new DOMParser().parseFromString(html, 'text/html');
    return bundleUrl(document.querySelector('script[type="module"][src]'));
}

function currentBundleUrl(): string | null {
    return bundleUrl(document.querySelector('script[type="module"][src]'));
}

function bundleUrl(script: Element | null): string | null {
    return script?.getAttribute('src') ?? null;
}
