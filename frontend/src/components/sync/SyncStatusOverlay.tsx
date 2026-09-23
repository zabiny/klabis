import {type ReactElement} from 'react';
import {Badge, Modal} from '../UI';
import {HalFormButton} from '../HalNavigator2/HalFormButton.tsx';
import {useHalRoute} from '../../contexts/halRouteContext';
import {formatDateTime, formatDateTimeSeconds} from '../../utils/dateUtils';
import {labels, getEnumLabel} from '../../localization';
import {SYNC_STATUS_MAP} from './syncStatusMap';
import type {GetSyncStateResource} from '../../api';

export interface SyncStatusOverlayProps {
    isOpen: boolean;
    onClose: () => void;
}

const DIRECTION_LABEL: Record<'INWARD' | 'OUTWARD', string> = {
    INWARD: labels.sync.directionInward,
    OUTWARD: labels.sync.directionOutward,
};

const SIDE_LABEL: Record<'LOCAL' | 'EXTERNAL' | 'BOTH', string> = {
    LOCAL: labels.sync.divergenceSideLocal,
    EXTERNAL: labels.sync.divergenceSideExternal,
    BOTH: labels.sync.divergenceSideBoth,
};

const getFieldValue = (
    projection: Record<string, unknown> | null | undefined,
    field: string,
): string => {
    if (!projection) return '—';
    const raw = projection[field];
    if (raw === null || raw === undefined) return '—';
    if (typeof raw === 'string') return raw;
    return JSON.stringify(raw);
};

export const SyncStatusOverlay = ({isOpen, onClose}: SyncStatusOverlayProps): ReactElement | null => {
    const {resourceData} = useHalRoute();
    const syncState = resourceData as GetSyncStateResource | null;

    if (!isOpen || !syncState) return null;

    const {variant, Icon} = SYNC_STATUS_MAP[syncState.status];
    const hasManagerFields = syncState.externalId !== undefined
        || syncState.lastDirection !== undefined
        || syncState.nextAttemptDueAt !== undefined
        || syncState.failedAttemptsSinceLastSuccess !== undefined;
    const divergedFields = syncState.status === 'CONFLICT' && syncState.divergedFields?.length
        ? syncState.divergedFields
        : null;
    const lastSyncText = syncState.lastSuccessfulSyncAt
        ? formatDateTimeSeconds(syncState.lastSuccessfulSyncAt)
        : labels.sync.neverSynced;

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title={labels.sync.overlayTitle}
            size="2xl"
        >
            <div data-testid="sync-overlay-modal" className="flex flex-col gap-6">
                <div className="flex items-center gap-3" data-testid="sync-overlay-status-header">
                    <Badge variant={variant} size="sm" className="inline-flex items-center gap-1.5">
                        <Icon className="w-4 h-4"/>
                        <span>{getEnumLabel('syncStatus', syncState.status)}</span>
                    </Badge>
                </div>

                <div className="flex flex-col gap-1">
                    <p className="text-sm font-medium text-text-secondary">{labels.sync.sectionLastSync}</p>
                    <p data-testid="sync-overlay-last-sync" className="text-text-primary">{lastSyncText}</p>
                </div>

                {hasManagerFields && (
                    <div className="flex flex-col gap-2" data-testid="sync-overlay-manager-details">
                        <p className="text-sm font-medium text-text-secondary">{labels.sync.sectionManagerDetails}</p>
                        <dl className="grid grid-cols-[max-content_1fr] gap-x-4 gap-y-1 text-sm">
                            {syncState.externalId !== undefined && (
                                <>
                                    <dt className="text-text-secondary">{labels.sync.externalId}</dt>
                                    <dd data-testid="sync-overlay-external-id" className="text-text-primary">{syncState.externalId}</dd>
                                </>
                            )}
                            {syncState.lastDirection !== undefined && syncState.lastDirection !== null && (
                                <>
                                    <dt className="text-text-secondary">{labels.sync.direction}</dt>
                                    <dd data-testid="sync-overlay-direction" className="text-text-primary">
                                        {DIRECTION_LABEL[syncState.lastDirection] ?? syncState.lastDirection}
                                    </dd>
                                </>
                            )}
                            {syncState.nextAttemptDueAt !== undefined && syncState.nextAttemptDueAt !== null && (
                                <>
                                    <dt className="text-text-secondary">{labels.sync.nextAttemptDueAt}</dt>
                                    <dd className="text-text-primary">{formatDateTime(syncState.nextAttemptDueAt)}</dd>
                                </>
                            )}
                            {syncState.failedAttemptsSinceLastSuccess !== undefined && (
                                <>
                                    <dt className="text-text-secondary">{labels.sync.failedAttemptsSinceLastSuccess}</dt>
                                    <dd data-testid="sync-overlay-failed-attempts" className="text-text-primary">
                                        {syncState.failedAttemptsSinceLastSuccess}
                                    </dd>
                                </>
                            )}
                        </dl>
                    </div>
                )}

                {divergedFields && (
                    <div className="flex flex-col gap-2" data-testid="sync-overlay-divergence">
                        <p className="text-sm font-medium text-text-secondary">{labels.sync.divergenceHeading}</p>
                        <table className="w-full text-sm border-collapse">
                            <thead>
                                <tr className="text-left text-text-secondary">
                                    <th className="border-b border-border py-1 pr-3">{labels.sync.divergenceField}</th>
                                    <th className="border-b border-border py-1 pr-3">{labels.sync.divergenceLocal}</th>
                                    <th className="border-b border-border py-1 pr-3">{labels.sync.divergenceExternal}</th>
                                    <th className="border-b border-border py-1 pr-3">{labels.sync.divergenceBaseline}</th>
                                    <th className="border-b border-border py-1">{labels.sync.divergenceSide}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {divergedFields.map((field) => (
                                    <tr key={field}>
                                        <td className="border-b border-border py-1 pr-3 font-mono">{field}</td>
                                        <td className="border-b border-border py-1 pr-3">{getFieldValue(syncState.local, field)}</td>
                                        <td className="border-b border-border py-1 pr-3">{getFieldValue(syncState.external, field)}</td>
                                        <td className="border-b border-border py-1 pr-3">{getFieldValue(syncState.baseline, field)}</td>
                                        <td className="border-b border-border py-1">{SIDE_LABEL[syncState.changedSides?.[field] ?? 'BOTH'] ?? syncState.changedSides?.[field] ?? 'BOTH'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                <div className="flex flex-col gap-2">
                    <p className="text-sm font-medium text-text-secondary">{labels.sync.actionsHeading}</p>
                    <div className="flex flex-wrap gap-2">
                        <HalFormButton name="synchronizeNow" modal={true} variant="primary"/>
                        <HalFormButton name="acknowledgeSyncConflict" modal={true} variant="secondary"/>
                        <HalFormButton name="resolveSyncConflict" modal={true} variant="secondary"/>
                        <HalFormButton name="resetSyncRecord" modal={true} variant="danger"/>
                    </div>
                </div>
            </div>
        </Modal>
    );
};

SyncStatusOverlay.displayName = 'SyncStatusOverlay';
