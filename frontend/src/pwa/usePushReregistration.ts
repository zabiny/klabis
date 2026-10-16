import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
import {authorizedFetch} from '../api/authorizedFetch';
import type {HalResponse} from '../api';
import {useAuthorizedQuery} from '../hooks/useAuthorizedFetch';
import {PUSH_SUBSCRIPTION_CHANGED} from './pushHandlers';
import {getRegisteredEndpoint, setRegisteredEndpoint} from './pushSubscriptionStore';
import {getServiceWorkerRegistration} from './registerServiceWorker';

type Settings = {
    devices?: {endpoint?: string}[];
    _templates?: {registerNotificationDevice?: {target?: string}};
};

async function reregisterIfEndpointChanged(settingsHref: string): Promise<boolean> {
    const remembered = getRegisteredEndpoint();
    if (!remembered || typeof Notification === 'undefined' || Notification.permission !== 'granted') {
        return false;
    }
    const registration = await getServiceWorkerRegistration();
    const subscription = await registration?.pushManager.getSubscription();
    if (!subscription || subscription.endpoint === remembered) {
        return false;
    }

    const settings: Settings = await (await authorizedFetch(settingsHref)).json();
    const known = settings.devices?.some(device => device.endpoint === subscription.endpoint);
    const target = settings._templates?.registerNotificationDevice?.target;
    if (!known) {
        if (!target) {
            return false;
        }
        const {p256dh, auth} = subscription.toJSON().keys ?? {};
        await authorizedFetch(target, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({endpoint: subscription.endpoint, p256dh, auth}),
        });
    }
    setRegisteredEndpoint(subscription.endpoint);
    return true;
}

export function usePushReregistration(): void {
    const queryClient = useQueryClient();
    const {data: settingsHref} = useAuthorizedQuery<string | undefined>('/api', {
        select: (root) => {
            const link = (root as HalResponse)._links?.notifications;
            return (Array.isArray(link) ? link[0] : link)?.href;
        },
        staleTime: 5 * 60 * 1000,
    });

    useEffect(() => {
        if (!settingsHref) {
            return;
        }
        const check = () => {
            reregisterIfEndpointChanged(settingsHref)
                .then(changed => {
                    if (changed) {
                        void queryClient.invalidateQueries({queryKey: ['authorized']});
                    }
                })
                .catch(() => undefined);
        };
        const onMessage = (event: MessageEvent) => {
            if (event.data?.type === PUSH_SUBSCRIPTION_CHANGED) {
                check();
            }
        };
        check();
        navigator.serviceWorker?.addEventListener('message', onMessage);
        return () => navigator.serviceWorker?.removeEventListener('message', onMessage);
    }, [settingsHref, queryClient]);
}
