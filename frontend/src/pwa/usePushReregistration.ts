import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
import {authorizedFetch} from '../api/authorizedFetch';
import {parseHalHref, templateTarget} from '../api/hateoas';
import type {components} from '../api/klabisApi';
import {PUSH_SUBSCRIPTION_CHANGED} from './pushHandlers';
import {getCurrentPushSubscription, toRegisterDeviceRequest} from './pushSubscription';
import {getRegisteredEndpoint, setRegisteredEndpoint} from './pushSubscriptionStore';

type NotificationSettings = components['schemas']['EntityModelNotificationSettings'];

async function reregisterIfEndpointChanged(settingsHref: string): Promise<boolean> {
    const remembered = getRegisteredEndpoint();
    if (!remembered || typeof Notification === 'undefined' || Notification.permission !== 'granted') {
        return false;
    }
    const subscription = await getCurrentPushSubscription();
    if (!subscription || subscription.endpoint === remembered) {
        return false;
    }

    const settings: NotificationSettings = await (await authorizedFetch(settingsHref)).json();
    const known = settings.devices?.some(device => device.endpoint === subscription.endpoint);
    if (!known) {
        const target = templateTarget(settings._templates?.registerNotificationDevice, settings);
        if (!target) {
            return false;
        }
        await authorizedFetch(target, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(toRegisterDeviceRequest(subscription)),
        });
    }
    setRegisteredEndpoint(subscription.endpoint);
    return true;
}

export function usePushReregistration(settingsHref: string | undefined): void {
    const queryClient = useQueryClient();

    useEffect(() => {
        if (!settingsHref) {
            return;
        }
        const check = () => {
            reregisterIfEndpointChanged(settingsHref)
                .then(changed => {
                    if (changed) {
                        void queryClient.invalidateQueries({queryKey: ['authorized', parseHalHref(settingsHref).pathname]});
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
