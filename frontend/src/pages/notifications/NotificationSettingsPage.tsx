import {type ReactElement, useState} from 'react';
import type {HalEnvelope} from '../../api';
import type {components} from '../../api/klabisApi';
import {Alert, Badge, Button, Card, Skeleton} from '../../components/UI';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
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
    const {mutate, isPending} = useAuthorizedMutation({method: 'POST'});

    const state = permissionRefused && detectedState === 'off' ? 'denied' : detectedState;
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
            {enableFailed && <Alert severity="error">{labels.notifications.enableFailed}</Alert>}

            {state === 'off' && registerTemplate && (
                <div>
                    <Button onClick={handleEnable} disabled={isPending}>
                        {labels.notifications.enableOnThisDevice}
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
                        {devices.map(device => (
                            <div key={device.id} className="flex items-center justify-between gap-4 p-4">
                                <div>
                                    <p className="font-medium text-text-primary">{device.description}</p>
                                    {device.enabledAt && (
                                        <p className="text-sm text-text-secondary">
                                            {labels.notifications.enabledAt}: {formatDateTime(device.enabledAt)}
                                        </p>
                                    )}
                                </div>
                                {currentEndpoint && device.endpoint === currentEndpoint && (
                                    <Badge variant="success">{labels.notifications.currentDevice}</Badge>
                                )}
                            </div>
                        ))}
                    </Card>
                )}
            </div>
        </div>
    );
};
