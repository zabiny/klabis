import {type ReactElement, useState} from 'react';
import type {HalEnvelope} from '../../api';
import {templateTarget} from '../../api/hateoas';
import type {components} from '../../api/klabisApi';
import {Alert, Badge, Button, Card, Skeleton} from '../../components/UI';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {useHalPageData} from '../../hooks/useHalPageData';
import {labels} from '../../localization';
import {
    base64UrlToBytes,
    toRegisterDeviceRequest,
    unsubscribeCurrentDevice,
} from '../../pwa/pushSubscription';
import {setRegisteredEndpoint} from '../../pwa/pushSubscriptionStore';
import {getServiceWorkerRegistration} from '../../pwa/registerServiceWorker';
import {formatDateTime} from '../../utils/dateUtils';
import {type BrowserNotificationState, useDeviceNotificationState} from './useDeviceNotificationState';

type NotificationSettings = components['schemas']['EntityModelNotificationSettings'];
type Device = NonNullable<NotificationSettings['devices']>[number];
type Severity = 'info' | 'warning' | 'success' | 'error';
type Feedback = {severity: Severity; text: string} | null;

const BROWSER_STATE_MESSAGES: Record<Exclude<BrowserNotificationState, 'loading' | 'ready'>, {text: string; severity: Severity}> = {
    unsupported: {text: labels.notifications.stateUnsupported, severity: 'warning'},
    'ios-needs-install': {text: labels.notifications.stateIosNeedsInstall, severity: 'info'},
    denied: {text: labels.notifications.stateDenied, severity: 'warning'},
};

export const NotificationSettingsPage = (): ReactElement => {
    const {resourceData, isLoading, route} = useHalPageData<NotificationSettings & HalEnvelope>();
    const devices = resourceData?.devices ?? [];
    const {state, browserEndpoint, refresh} = useDeviceNotificationState();
    const [feedback, setFeedback] = useState<Feedback>(null);
    const {mutate, isPending} = useAuthorizedMutation({method: 'POST'});
    const {mutate: mutateRemove, isPending: isRemoving} = useAuthorizedMutation({method: 'DELETE'});

    const currentDevice = browserEndpoint ? devices.find(device => device.endpoint === browserEndpoint) : undefined;
    const registerTarget = templateTarget(resourceData?._templates?.registerNotificationDevice, resourceData);
    const sendTestTarget = templateTarget(resourceData?._templates?.sendTestNotification, resourceData);
    const removeTarget = (device: Device) => templateTarget(device._templates?.removeNotificationDevice, device);

    const handleSendTest = () => {
        if (!sendTestTarget) {
            return;
        }
        setFeedback(null);
        mutate({url: sendTestTarget}, {
            onSuccess: () => setFeedback({severity: 'success', text: labels.notifications.sendTestSuccess}),
            onError: () => setFeedback({severity: 'error', text: labels.notifications.sendTestFailed}),
        });
    };

    const handleRemove = (device: Device, isCurrent: boolean) => {
        const url = removeTarget(device);
        if (!url) {
            return;
        }
        setFeedback(null);
        mutateRemove({url}, {
            onSuccess: async () => {
                if (isCurrent) {
                    await unsubscribeCurrentDevice();
                    refresh();
                }
                void route.refetch();
            },
            onError: () => setFeedback({
                severity: 'error',
                text: isCurrent ? labels.notifications.disableFailed : labels.notifications.removeFailed,
            }),
        });
    };

    const enableFailed = () => setFeedback({severity: 'error', text: labels.notifications.enableFailed});

    const handleEnable = async () => {
        setFeedback(null);
        try {
            if (await Notification.requestPermission() !== 'granted') {
                setFeedback(BROWSER_STATE_MESSAGES.denied);
                refresh();
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
            if (!registerTarget) {
                throw new Error('Missing registration target');
            }
            mutate({url: registerTarget, data: toRegisterDeviceRequest(subscription)}, {
                onSuccess: () => {
                    setRegisteredEndpoint(subscription.endpoint);
                    refresh();
                    void route.refetch();
                },
                onError: enableFailed,
            });
        } catch {
            enableFailed();
        }
    };

    if (isLoading || !resourceData) {
        return <Skeleton/>;
    }

    const stateMessage = state === 'loading' ? null
        : state === 'ready' ? (currentDevice
            ? {text: labels.notifications.stateOn, severity: 'success' as const}
            : {text: labels.notifications.stateOff, severity: 'info' as const})
            : BROWSER_STATE_MESSAGES[state];

    return (
        <div className="flex flex-col gap-6">
            <h1 className="text-3xl font-bold text-text-primary">{labels.notifications.pageTitle}</h1>

            {stateMessage && <Alert severity={stateMessage.severity}>{stateMessage.text}</Alert>}
            {feedback && <Alert severity={feedback.severity}>{feedback.text}</Alert>}

            {state === 'ready' && !currentDevice && registerTarget && (
                <div>
                    <Button onClick={handleEnable} disabled={isPending}>
                        {labels.notifications.enableOnThisDevice}
                    </Button>
                </div>
            )}

            {currentDevice && removeTarget(currentDevice) && (
                <div>
                    <Button variant="secondary" onClick={() => handleRemove(currentDevice, true)} disabled={isRemoving}>
                        {labels.notifications.disableOnThisDevice}
                    </Button>
                </div>
            )}

            {sendTestTarget && (
                <div className="flex flex-col gap-3">
                    <div>
                        <Button variant="secondary" onClick={handleSendTest} disabled={isPending}>
                            {labels.notifications.sendTest}
                        </Button>
                    </div>
                </div>
            )}

            <div className="flex flex-col gap-3">
                <h2 className="text-xl font-bold text-text-primary">{labels.notifications.devicesHeading}</h2>
                {devices.length === 0 ? (
                    <p className="text-text-secondary">{labels.notifications.noDevices}</p>
                ) : (
                    <Card className="divide-y divide-border">
                        {devices.map(device => {
                            const isCurrent = device === currentDevice;
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
