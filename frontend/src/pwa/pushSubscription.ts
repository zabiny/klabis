import {clearRegisteredEndpoint} from './pushSubscriptionStore';
import {getServiceWorkerRegistration} from './registerServiceWorker';

export interface RegisterDeviceRequest {
    endpoint: string;
    p256dh?: string;
    auth?: string;
}

export async function getCurrentPushSubscription(): Promise<PushSubscription | null> {
    const registration = await getServiceWorkerRegistration();
    return (await registration?.pushManager.getSubscription()) ?? null;
}

export async function unsubscribeCurrentDevice(): Promise<void> {
    clearRegisteredEndpoint();
    try {
        const subscription = await getCurrentPushSubscription();
        await subscription?.unsubscribe();
    } catch {
        // the server row is already gone or irrelevant; a leftover browser subscription is harmless
    }
}

export function toRegisterDeviceRequest(subscription: PushSubscription): RegisterDeviceRequest {
    const {p256dh, auth} = subscription.toJSON().keys ?? {};
    return {endpoint: subscription.endpoint, p256dh, auth};
}

export function base64UrlToBytes(value: string): Uint8Array<ArrayBuffer> {
    const base64 = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
    const binary = atob(base64);
    return Uint8Array.from(binary, char => char.charCodeAt(0));
}
