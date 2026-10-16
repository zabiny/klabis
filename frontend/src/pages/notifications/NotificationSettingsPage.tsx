import type {ReactElement} from 'react';
import type {components} from '../../api/klabisApi';
import {Alert, Badge, Card, Skeleton} from '../../components/UI';
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

export const NotificationSettingsPage = (): ReactElement => {
    const {resourceData, isLoading} = useHalPageData<NotificationSettings>();
    const devices = resourceData?.devices ?? [];
    const {state, currentEndpoint} = useDeviceNotificationState(
        devices.flatMap(device => (device.endpoint ? [device.endpoint] : []))
    );

    if (isLoading || !resourceData) {
        return <Skeleton/>;
    }

    const message = state === 'loading' ? null : STATE_MESSAGES[state];

    return (
        <div className="flex flex-col gap-6">
            <h1 className="text-3xl font-bold text-text-primary">{labels.notifications.pageTitle}</h1>

            {message && <Alert severity={message.severity}>{message.text}</Alert>}

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
