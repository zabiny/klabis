import {useEffect, type ReactElement} from 'react';
import {Modal} from '../UI/Modal';
import {Button} from '../UI/Button';
import {Spinner} from '../UI/Spinner';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation';
import {labels} from '../../localization';

interface BulkSyncResult {
    totalProcessed: number;
    successCount: number;
    failureCount: number;
    results: Array<{
        eventId: string;
        name: string;
        status: 'synced' | 'failed';
        error?: string;
    }>;
}

export interface BulkSyncOrisModalProps {
    isOpen: boolean;
    onClose: () => void;
    syncUrl: string | undefined;
    onSyncComplete: () => void;
}

export const BulkSyncOrisModal = ({isOpen, onClose, syncUrl, onSyncComplete}: BulkSyncOrisModalProps): ReactElement | null => {
    const {mutate, reset, isPending, data, isSuccess} = useAuthorizedMutation({
        method: 'POST',
    });
    const {invalidateAllCaches} = useFormCacheInvalidation();

    useEffect(() => {
        if (isOpen && syncUrl) {
            reset();
            mutate({url: syncUrl}, {
                onSuccess: async () => {
                    await invalidateAllCaches();
                    onSyncComplete();
                },
            });
        }
    // syncUrl and onSyncComplete are stable for a given modal open — intentionally excluded
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isOpen]);

    if (!isOpen) return null;

    const result = isSuccess ? (data?.data as BulkSyncResult | undefined) : undefined;
    const failures = result?.results.filter(r => r.status === 'failed') ?? [];

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title={labels.dialogTitles.syncAllUpcomingFromOris}
            closeButton={!isPending}
            closeOnBackdropClick={!isPending}
            size="md"
            footer={
                isSuccess ? (
                    <Button variant="secondary" onClick={onClose}>
                        {labels.buttons.close}
                    </Button>
                ) : undefined
            }
        >
            {isPending && (
                <div className="flex flex-col items-center gap-4 py-4">
                    <Spinner size="lg" />
                    <p className="text-text-secondary">{labels.bulkSync.progress}</p>
                </div>
            )}

            {isSuccess && result && (
                <div className="flex flex-col gap-4">
                    <p className="text-text-primary">
                        {labels.bulkSync.successCount(result.successCount)}
                        {result.failureCount > 0 && (
                            <>, {labels.bulkSync.failureCount(result.failureCount)}</>
                        )}
                    </p>

                    {failures.length > 0 && (
                        <div className="flex flex-col gap-2">
                            <p className="text-sm font-medium text-text-secondary">{labels.bulkSync.failuresHeading}</p>
                            <ul className="flex flex-col gap-1">
                                {failures.map(f => (
                                    <li key={f.eventId} className="text-sm">
                                        <span className="font-medium text-red-600">{f.name}</span>
                                        {f.error && <>: <span className="text-text-secondary">{f.error}</span></>}
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>
            )}
        </Modal>
    );
};
