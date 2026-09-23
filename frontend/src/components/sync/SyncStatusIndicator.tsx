import {type ReactElement, useState} from 'react';
import {AlertTriangle} from 'lucide-react';
import {Badge, Spinner, Tooltip} from '../UI';
import {HalRouteProvider} from '../../contexts/HalRouteContext';
import {useHalRoute} from '../../contexts/halRouteContext';
import {formatDate} from '../../utils/dateUtils';
import {getEnumLabel, labels} from '../../localization';
import {SyncStatusOverlay} from './SyncStatusOverlay';
import {SYNC_STATUS_MAP} from './syncStatusMap';
import type {GetSyncStateResource, HalResourceLinks} from '../../api';

export interface SyncStatusIndicatorProps {
    syncLink: HalResourceLinks | undefined | null;
    mode: 'icon' | 'icon+date';
}

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
    const [isOpen, setIsOpen] = useState(false);

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

    const {variant, Icon} = SYNC_STATUS_MAP[syncState.status];
    const lastDate = syncState.lastSuccessfulSyncAt;
    const dateText = lastDate ? formatDate(lastDate) : labels.sync.neverSynced;
    const showDateInline = mode === 'icon+date';
    const hasTemplates = syncState._templates !== undefined
        && Object.keys(syncState._templates).length > 0;

    const badge = (
        <Badge
            variant={variant}
            size="sm"
            className={showDateInline ? 'inline-flex items-center gap-1.5' : 'inline-flex items-center justify-center'}
            data-testid={`sync-status-${syncState.status}`}
            aria-label={getEnumLabel('syncStatus', syncState.status)}
            role={hasTemplates ? 'button' : undefined}
            tabIndex={hasTemplates ? 0 : undefined}
            onClick={hasTemplates ? (e) => {
                e.stopPropagation();
                setIsOpen(true);
            } : undefined}
            onKeyDown={hasTemplates ? (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    e.stopPropagation();
                    setIsOpen(true);
                }
            } : undefined}
        >
            <Icon className={ICON_CLASS}/>
            {showDateInline && <span data-testid="sync-last-date">{dateText}</span>}
        </Badge>
    );

    const overlay = hasTemplates && isOpen
        ? <SyncStatusOverlay isOpen={isOpen} onClose={() => setIsOpen(false)}/>
        : null;

    if (showDateInline) {
        return (
            <>
                {badge}
                {overlay}
            </>
        );
    }

    return (
        <>
            <Tooltip content={dateText}>{badge}</Tooltip>
            {overlay}
        </>
    );
};

SyncStatusIndicatorContent.displayName = 'SyncStatusIndicatorContent';
