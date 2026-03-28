import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Modal} from '../UI/Modal';
import {Button} from '../UI/Button';
import {authorizedFetch} from '../../api/authorizedFetch';
import {labels} from '../../localization';
import {extractNavigationPath} from '../../utils/navigationPath';

interface OrisEvent {
    id: number;
    name: string;
    date: string;
}

interface ImportOrisEventModalProps {
    isOpen: boolean;
    onClose: () => void;
    importHref: string;
}

type FetchState = 'loading' | 'success' | 'error';

export const ImportOrisEventModal = ({isOpen, onClose, importHref}: ImportOrisEventModalProps) => {
    const navigate = useNavigate();
    const [fetchState, setFetchState] = useState<FetchState>('loading');
    const [orisEvents, setOrisEvents] = useState<OrisEvent[]>([]);
    const [selectedId, setSelectedId] = useState<string>('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    useEffect(() => {
        if (!isOpen) return;

        setFetchState('loading');
        setOrisEvents([]);
        setSelectedId('');
        setSubmitError(null);

        authorizedFetch('/api/oris/events')
            .then((res) => res.json())
            .then((data: OrisEvent[]) => {
                setOrisEvents(data);
                setFetchState('success');
            })
            .catch(() => {
                setFetchState('error');
            });
    }, [isOpen]);

    const handleSubmit = async () => {
        if (!selectedId) return;

        setIsSubmitting(true);
        setSubmitError(null);

        try {
            const response = await authorizedFetch(
                importHref,
                {
                    method: 'POST',
                    body: JSON.stringify({orisId: Number(selectedId)}),
                    headers: {'Content-Type': 'application/json'},
                },
                false,
            );

            if (response.status === 201) {
                const location = response.headers.get('location');
                onClose();
                if (location) {
                    navigate(extractNavigationPath(location));
                }
            } else if (response.status === 409) {
                setSubmitError(labels.errors.importOrisConflict);
            } else {
                setSubmitError(labels.errors.importOrisFailed);
            }
        } catch {
            setSubmitError(labels.errors.importOrisFailed);
        } finally {
            setIsSubmitting(false);
        }
    };

    const isSubmitDisabled =
        fetchState !== 'success' || orisEvents.length === 0 || !selectedId || isSubmitting;

    const footer = (
        <>
            <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
                {labels.buttons.cancel}
            </Button>
            <Button onClick={handleSubmit} disabled={isSubmitDisabled} loading={isSubmitting}>
                {isSubmitting ? labels.buttons.importing : labels.templates.importEvent}
            </Button>
        </>
    );

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title={labels.dialogTitles.importEvent}
            footer={footer}
            closeOnBackdropClick={!isSubmitting}
        >
            {fetchState === 'loading' && (
                <p className="text-text-secondary">{labels.ui.loading}</p>
            )}

            {fetchState === 'error' && (
                <p className="text-error">{labels.errors.importOrisLoadFailed}</p>
            )}

            {fetchState === 'success' && orisEvents.length === 0 && (
                <p className="text-text-secondary">{labels.errors.importOrisNoEvents}</p>
            )}

            {fetchState === 'success' && orisEvents.length > 0 && (
                <select
                    className="w-full rounded-md border border-border bg-surface px-3 py-2 text-text-primary focus:outline-none focus:ring-2 focus:ring-accent"
                    value={selectedId}
                    onChange={(e) => setSelectedId(e.target.value)}
                    aria-label="Vyberte závod k importu"
                >
                    <option value="">— Vyberte závod —</option>
                    {orisEvents.map((event) => (
                        <option key={event.id} value={String(event.id)}>
                            {event.date} — {event.name}
                        </option>
                    ))}
                </select>
            )}

            {submitError && (
                <p className="mt-3 text-error">{submitError}</p>
            )}
        </Modal>
    );
};
