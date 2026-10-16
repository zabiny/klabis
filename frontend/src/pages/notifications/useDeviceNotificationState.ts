import {useEffect, useState} from 'react';
import {getServiceWorkerRegistration} from '../../pwa/registerServiceWorker';

export type DeviceNotificationState = 'loading' | 'unsupported' | 'ios-needs-install' | 'denied' | 'off' | 'on';

export interface DeviceNotificationStatus {
    state: DeviceNotificationState;
    currentEndpoint: string | null;
}

const isIos = () => /iPad|iPhone|iPod/.test(navigator.userAgent);

const isStandalone = () =>
    window.matchMedia?.('(display-mode: standalone)').matches === true ||
    (navigator as Navigator & {standalone?: boolean}).standalone === true;

async function currentEndpoint(): Promise<string | null> {
    const registration = await getServiceWorkerRegistration();
    const subscription = await registration?.pushManager.getSubscription();
    return subscription?.endpoint ?? null;
}

async function resolveStatus(deviceEndpoints: string[]): Promise<DeviceNotificationStatus> {
    if (!('PushManager' in window) || !('Notification' in window)) {
        return {state: 'unsupported', currentEndpoint: null};
    }
    if (isIos() && !isStandalone()) {
        return {state: 'ios-needs-install', currentEndpoint: null};
    }
    if (Notification.permission === 'denied') {
        return {state: 'denied', currentEndpoint: null};
    }
    const endpoint = await currentEndpoint().catch(() => null);
    if (endpoint && deviceEndpoints.includes(endpoint)) {
        return {state: 'on', currentEndpoint: endpoint};
    }
    return {state: 'off', currentEndpoint: null};
}

export function useDeviceNotificationState(deviceEndpoints: string[]): DeviceNotificationStatus {
    const [status, setStatus] = useState<DeviceNotificationStatus>({state: 'loading', currentEndpoint: null});
    const key = deviceEndpoints.join('\n');

    useEffect(() => {
        let cancelled = false;
        resolveStatus(key ? key.split('\n') : []).then(result => {
            if (!cancelled) setStatus(result);
        });
        return () => {
            cancelled = true;
        };
    }, [key]);

    return status;
}
