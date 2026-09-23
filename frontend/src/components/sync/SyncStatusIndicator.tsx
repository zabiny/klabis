import {type ReactElement} from 'react';
import {
    AlertTriangle,
    Archive,
    Check,
    CircleDot,
    RotateCw,
    XCircle,
} from 'lucide-react';
import {Badge, Spinner, Tooltip} from '../UI';
import {HalRouteProvider} from '../../contexts/HalRouteContext';
import {useHalRoute} from '../../contexts/halRouteContext';
import {formatDate} from '../../utils/dateUtils';
import {labels} from '../../localization';
import type {GetSyncStateResource, HalResourceLinks} from '../../api';

export interface SyncStatusIndicatorProps {
    syncLink: HalResourceLinks | undefined | null;
    mode: 'icon' | 'icon+date';
}

type SyncStatus = GetSyncStateResource['status'];
type StatusVariant = 'info' | 'success' | 'warning' | 'error' | 'default';

const STATUS_MAP: Record<SyncStatus, {
    variant: StatusVariant;
    Icon: typeof CircleDot;
    iconName: string;
}> = {
    NEW: {variant: 'info', Icon: CircleDot, iconName: 'CircleDot'},
    IN_SYNC: {variant: 'success', Icon: Check, iconName: 'Check'},
    RETRYING: {variant: 'warning', Icon: RotateCw, iconName: 'RotateCw'},
    CONFLICT: {variant: 'error', Icon: AlertTriangle, iconName: 'AlertTriangle'},
    FAILED: {variant: 'error', Icon: XCircle, iconName: 'XCircle'},
    RETIRED: {variant: 'default', Icon: Archive, iconName: 'Archive'},
};

const ICON_CLASS = 'w-4 h-4';

export const SyncStatusIndicator = ({syncLink, mode}: SyncStatusIndicatorProps): ReactElement | null => {
    if (!syncLink) return null;
    return (
        <HalRouteProvider routeLink={syncLink}>
            <SyncStatusIndicatorContent mode={mode}/>
        </HalRouteProvider>
    );
};

SyncStatusIndicator.displayName = 'SyncStatusIndicator';

const SyncStatusIndicatorContent = ({mode}: {mode: 'icon' | 'icon+date'}): ReactElement => {
    const {resourceData, isLoading, error} = useHalRoute();
    const syncState = resourceData as GetSyncStateResource | null;

    if (isLoading) {
        return (
            <span className="inline-flex items-center" data-testid="sync-loading">
                <Spinner size="sm"/>
            </span>
        );
    }

    if (error || !syncState?.status) {
        return (
            <span
                role="alert"
                aria-label="Sync status unavailable"
                data-testid="sync-error"
                className="inline-flex items-center text-error"
            >
                <AlertTriangle className={ICON_CLASS}/>
            </span>
        );
    }

    const {variant, Icon, iconName} = STATUS_MAP[syncState.status];
    const lastDate = syncState.lastSuccessfulSyncAt;
    const dateText = lastDate ? formatDate(lastDate) : labels.sync.neverSynced;
    const showDateInline = mode === 'icon+date';

    const badge = (
        <Badge
            variant={variant}
            size="sm"
            className={showDateInline ? 'inline-flex items-center gap-1.5' : 'inline-flex items-center justify-center'}
            data-testid={`sync-status-${syncState.status}`}
            aria-label={`${iconName} ${syncState.status}`}
        >
            <Icon className={ICON_CLASS}/>
            {showDateInline && <span data-testid="sync-last-date">{dateText}</span>}
        </Badge>
    );

    if (showDateInline) {
        return badge;
    }

    return <Tooltip content={dateText}>{badge}</Tooltip>;
};

SyncStatusIndicatorContent.displayName = 'SyncStatusIndicatorContent';
