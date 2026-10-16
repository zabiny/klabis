import {useCallback, useEffect, useState} from 'react';
import {getCurrentPushSubscription} from '../../pwa/pushSubscription';

export type BrowserNotificationState = 'loading' | 'unsupported' | 'ios-needs-install' | 'denied' | 'ready';

interface BrowserSnapshot {
    state: BrowserNotificationState;
    browserEndpoint: string | null;
}

export interface BrowserNotificationStatus extends BrowserSnapshot {
    refresh: () => void;
}

const isIos = () => /iPad|iPhone|iPod/.test(navigator.userAgent);

const isStandalone = () =>
    window.matchMedia?.('(display-mode: standalone)').matches === true ||
    (navigator as Navigator & {standalone?: boolean}).standalone === true;

async function resolveStatus(): Promise<BrowserSnapshot> {
    if (!('PushManager' in window) || !('Notification' in window)) {
        return {state: 'unsupported', browserEndpoint: null};
    }
    if (isIos() && !isStandalone()) {
        return {state: 'ios-needs-install', browserEndpoint: null};
    }
    if (Notification.permission === 'denied') {
        return {state: 'denied', browserEndpoint: null};
    }
    const subscription = await getCurrentPushSubscription().catch(() => null);
    return {state: 'ready', browserEndpoint: subscription?.endpoint ?? null};
}

export function useDeviceNotificationState(): BrowserNotificationStatus {
    const [snapshot, setSnapshot] = useState<BrowserSnapshot>({state: 'loading', browserEndpoint: null});
    const [revision, setRevision] = useState(0);

    useEffect(() => {
        let cancelled = false;
        resolveStatus().then(result => {
            if (!cancelled) setSnapshot(result);
        });
        return () => {
            cancelled = true;
        };
    }, [revision]);

    const refresh = useCallback(() => setRevision(value => value + 1), []);

    return {...snapshot, refresh};
}
