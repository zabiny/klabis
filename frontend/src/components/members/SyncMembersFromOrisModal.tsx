import {type ReactElement, useEffect} from 'react';
import {Modal} from '../UI/Modal';
import {Button} from '../UI/Button';
import {Spinner} from '../UI/Spinner';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation';
import {labels} from '../../localization';

export interface SyncMembersFromOrisModalProps {
    isOpen: boolean;
    onClose: () => void;
    targetUrl: string | undefined;
    onSyncComplete: () => void;
}

export const SyncMembersFromOrisModal = ({
    isOpen, onClose, targetUrl, onSyncComplete,
}: SyncMembersFromOrisModalProps): ReactElement | null => {
    const {mutate, reset, isPending, isSuccess, isError} = useAuthorizedMutation({method: 'POST'});
    const {invalidateAllCaches} = useFormCacheInvalidation();

    useEffect(() => {
        if (isOpen && targetUrl) {
            reset();
            mutate({url: targetUrl}, {
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

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title={labels.dialogTitles.importFromOris}
            closeButton={!isPending}
            closeOnBackdropClick={!isPending}
            size="md"
            footer={
                (isSuccess || isError) ? (
                    <Button variant="secondary" onClick={onClose}>
                        {labels.buttons.close}
                    </Button>
                ) : undefined
            }
        >
            {isPending && (
                <div className="flex flex-col items-center gap-4 py-4">
                    <Spinner size="lg"/>
                    <p className="text-text-secondary">{labels.bulkSync.progress}</p>
                </div>
            )}
            {isSuccess && (
                <p className="text-text-primary">Synchronizace členů z ORIS byla dokončena.</p>
            )}
            {isError && (
                <p className="text-red-600">Synchronizace selhala. Zkuste to prosím znovu.</p>
            )}
        </Modal>
    );
};