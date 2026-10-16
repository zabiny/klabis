const ICON = '/pwa-192x192.png';
const DEFAULT_TITLE = 'Klabis';
const HOME_URL = '/';

interface WindowClientLike {
    url: string;
    focus(): Promise<unknown>;
    navigate(url: string): Promise<unknown>;
}

export interface PushScope {
    location: {origin: string};
    registration: {showNotification(title: string, options?: NotificationOptions): Promise<void>};
    clients: {
        matchAll(options: {type: 'window'; includeUncontrolled: boolean}): Promise<readonly WindowClientLike[]>;
        openWindow(url: string): Promise<unknown>;
    };
}

interface PushEventLike {
    data: {json(): unknown} | null;
    waitUntil(promise: Promise<unknown>): void;
}

interface NotificationClickEventLike {
    notification: {close(): void; data?: unknown};
    waitUntil(promise: Promise<unknown>): void;
}

interface PushPayload {
    title?: unknown;
    body?: unknown;
    url?: unknown;
    tag?: unknown;
}

function asString(value: unknown): string | undefined {
    return typeof value === 'string' && value.length > 0 ? value : undefined;
}

function parsePayload(event: PushEventLike): PushPayload {
    try {
        const parsed = event.data?.json();
        return typeof parsed === 'object' && parsed !== null ? (parsed as PushPayload) : {};
    } catch {
        return {};
    }
}

export function handlePush(scope: PushScope, event: PushEventLike): void {
    const payload = parsePayload(event);
    const title = asString(payload.title) ?? DEFAULT_TITLE;
    const options: NotificationOptions = {
        body: asString(payload.body),
        icon: ICON,
        badge: ICON,
        tag: asString(payload.tag),
        data: {url: asString(payload.url) ?? HOME_URL},
    };
    event.waitUntil(scope.registration.showNotification(title, options));
}

export function handleNotificationClick(scope: PushScope, event: NotificationClickEventLike): void {
    event.notification.close();
    const data = event.notification.data as {url?: unknown} | undefined;
    const target = new URL(asString(data?.url) ?? HOME_URL, scope.location.origin).href;

    event.waitUntil(
        scope.clients.matchAll({type: 'window', includeUncontrolled: true}).then(async (windowClients) => {
            const sameOrigin = windowClients.find((c) => new URL(c.url).origin === scope.location.origin);
            if (!sameOrigin) {
                await scope.clients.openWindow(target);
                return;
            }
            await sameOrigin.focus();
            await sameOrigin.navigate(target);
        }),
    );
}
