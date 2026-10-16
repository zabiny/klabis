import {type ReactElement, useState} from 'react';
import type {HalEnvelope} from '../../api';
import type {components} from '../../api/klabisApi';
import {Alert, Badge, Button, Card, Skeleton} from '../../components/UI';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {clearRegisteredEndpoint, setRegisteredEndpoint} from '../../pwa/pushSubscriptionStore';
import {getServiceWorkerRegistration} from '../../pwa/registerServiceWorker';
import {useHalPageData} from '../../hooks/useHalPageData';
import {labels} from '../../localization';
import {formatDateTime} from '../../utils/dateUtils';
import {type DeviceNotificationState, useDeviceNotificationState} from './useDeviceNotificationState';

type NotificationSettings = components['schemas']['EntityModelNotificationSettings'];

const STATE_MESSAGES: Record<Exclude<DeviceNotificationState, 'loading'>, {text: string; severity: 'info' | 'warning' | 'success'}> = {
    unsupported: {text: labels.notifications.stateUnsupported, severity: 'warning'},
    'ios-needs-install': {text: labels.notifications.stateIosNeedsInstall, severity: 'info'},
    denied: {text: labels.notifications.stateDenied, severity: 'warning'},
    off: {text: labels.notifications.stateOff, severity: 'info'},
    on: {text: labels.notifications.stateOn, severity: 'success'},
};

function base64UrlToBytes(value: string): Uint8Array<ArrayBuffer> {
    const base64 = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
    const binary = atob(base64);
    return Uint8Array.from(binary, char => char.charCodeAt(0));
}

export const NotificationSettingsPage = (): ReactElement => {
    const {resourceData, isLoading, route} = useHalPageData<NotificationSettings & HalEnvelope>();
    const devices = resourceData?.devices ?? [];
    const {state: detectedState, currentEndpoint} = useDeviceNotificationState(
        devices.flatMap(device => (device.endpoint ? [device.endpoint] : []))
    );
    const [permissionRefused, setPermissionRefused] = useState(false);
    const [enableFailed, setEnableFailed] = useState(false);
    const [testResult, setTestResult] = useState<'success' | 'failed' | null>(null);
    const [removeError, setRemoveError] = useState<'disable' | 'remove' | null>(null);
    const {mutate, isPending} = useAuthorizedMutation({method: 'POST'});
    const {mutate: mutateRemove, isPending: isRemoving} = useAuthorizedMutation({method: 'DELETE'});

    const state = permissionRefused && detectedState === 'off' ? 'denied' : detectedState;
    const currentDevice = devices.find(device => !!currentEndpoint && device.endpoint === currentEndpoint);
    const registerTemplate = resourceData?._templates?.registerNotificationDevice;
    const sendTestTemplate = resourceData?._templates?.sendTestNotification;

    const handleSendTest = () => {
        if (!sendTestTemplate?.target) {
            return;
        }
        setTestResult(null);
        mutate({url: sendTestTemplate.target}, {
            onSuccess: () => setTestResult('success'),
            onError: () => setTestResult('failed'),
        });
    };

    const removeTarget = (device: (typeof devices)[number]) =>
        device._templates?.removeNotificationDevice?.target;

    const handleRemove = (device: (typeof devices)[number], isCurrent: boolean) => {
        const url = removeTarget(device);
        if (!url) {
            return;
        }
        setRemoveError(null);
        mutateRemove({url}, {
            onSuccess: async () => {
                if (isCurrent) {
                    clearRegisteredEndpoint();
                    try {
                        const registration = await getServiceWorkerRegistration();
                        const subscription = await registration?.pushManager.getSubscription();
                        await subscription?.unsubscribe();
                    } catch {
                        // the server row is already gone; a leftover browser subscription is harmless
                    }
                }
                void route.refetch();
            },
            onError: () => setRemoveError(isCurrent ? 'disable' : 'remove'),
        });
    };

    const handleEnable = async () => {
        setEnableFailed(false);
        try {
            if (await Notification.requestPermission() !== 'granted') {
                setPermissionRefused(true);
                return;
            }
            const registration = await getServiceWorkerRegistration();
            if (!registration || !resourceData?.vapidPublicKey) {
                throw new Error('Push subscription is not available');
            }
            const subscription = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: base64UrlToBytes(resourceData.vapidPublicKey),
            });
            const {p256dh, auth} = subscription.toJSON().keys ?? {};
            const target = registerTemplate?.target ?? route.getResourceLink()?.href;
            if (!target) {
                throw new Error('Missing registration target');
            }
            mutate({url: target, data: {endpoint: subscription.endpoint, p256dh, auth}}, {
                onSuccess: () => {
                    setRegisteredEndpoint(subscription.endpoint);
                    void route.refetch();
                },
                onError: () => setEnableFailed(true),
            });
        } catch {
            setEnableFailed(true);
        }
    };

    if (isLoading || !resourceData) {
        return <Skeleton/>;
    }

    const message = state === 'loading' ? null : STATE_MESSAGES[state];

    return (
        <div className="flex flex-col gap-6">
            <h1 className="text-3xl font-bold text-text-primary">{labels.notifications.pageTitle}</h1>

            {message && <Alert severity={message.severity}>{message.text}</Alert>}
            {removeError === 'disable' && <Alert severity="error">{labels.notifications.disableFailed}</Alert>}
            {removeError === 'remove' && <Alert severity="error">{labels.notifications.removeFailed}</Alert>}
            {enableFailed && <Alert severity="error">{labels.notifications.enableFailed}</Alert>}

            {state === 'off' && registerTemplate && (
                <div>
                    <Button onClick={handleEnable} disabled={isPending}>
                        {labels.notifications.enableOnThisDevice}
                    </Button>
                </div>
            )}

            {state === 'on' && currentDevice && removeTarget(currentDevice) && (
                <div>
                    <Button variant="secondary" onClick={() => handleRemove(currentDevice, true)} disabled={isRemoving}>
                        {labels.notifications.disableOnThisDevice}
                    </Button>
                </div>
            )}

            {sendTestTemplate && (
                <div className="flex flex-col gap-3">
                    <div>
                        <Button variant="secondary" onClick={handleSendTest} disabled={isPending}>
                            {labels.notifications.sendTest}
                        </Button>
                    </div>
                    {testResult === 'success' && <Alert severity="success">{labels.notifications.sendTestSuccess}</Alert>}
                    {testResult === 'failed' && <Alert severity="error">{labels.notifications.sendTestFailed}</Alert>}
                </div>
            )}

            <div className="flex flex-col gap-3">
                <h2 className="text-xl font-bold text-text-primary">{labels.notifications.devicesHeading}</h2>
                {devices.length === 0 ? (
                    <p className="text-text-secondary">{labels.notifications.noDevices}</p>
                ) : (
                    <Card className="divide-y divide-border">
                        {devices.map(device => {
                            const isCurrent = !!currentEndpoint && device.endpoint === currentEndpoint;
                            return (
                            <div key={device.id} className="flex items-center justify-between gap-4 p-4">
                                <div>
                                    <p className="font-medium text-text-primary">{device.description}</p>
                                    {device.enabledAt && (
                                        <p className="text-sm text-text-secondary">
                                            {labels.notifications.enabledAt}: {formatDateTime(device.enabledAt)}
                                        </p>
                                    )}
                                </div>
                                <div className="flex items-center gap-3">
                                    {isCurrent && <Badge variant="success">{labels.notifications.currentDevice}</Badge>}
                                    {removeTarget(device) && !isCurrent && (
                                        <Button variant="secondary" disabled={isRemoving}
                                                onClick={() => handleRemove(device, false)}>
                                            {labels.notifications.removeDevice}
                                        </Button>
                                    )}
                                </div>
                            </div>
                            );
                        })}
                    </Card>
                )}
            </div>
        </div>
    );
};
