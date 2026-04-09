import {useCallback, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Modal} from '../UI/Modal';
import {Button} from '../UI/Button';
import {RadioGroup} from '../UI/forms';
import {authorizedFetch} from '../../api/authorizedFetch';
import {labels} from '../../localization';
import {extractNavigationPath} from '../../utils/navigationPath';

interface OrisEvent {
    id: number;
    name: string;
    date: string;
    location: string | null;
    organizer: string | null;
}

const ORIS_REGION_KEYS = ['JM', 'M', 'ČR'] as const;

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
    const [selectedRegion, setSelectedRegion] = useState<string>('JM');
    const [selectedId, setSelectedId] = useState<string>('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    const fetchEvents = useCallback((region: string) => {
        setFetchState('loading');
        setOrisEvents([]);
        setSelectedId('');
        setSubmitError(null);

        const params = new URLSearchParams();
        params.append('region', region);

        authorizedFetch(`/api/oris/events?${params.toString()}`)
            .then((res) => res.json())
            .then((data: OrisEvent[]) => {
                setOrisEvents(data);
                setFetchState('success');
            })
            .catch(() => {
                setFetchState('error');
            });
    }, []);

    useEffect(() => {
        if (!isOpen) return;
        setSelectedRegion('JM');
        fetchEvents('JM');
    }, [isOpen, fetchEvents]);

    const handleRegionChange = (regionValue: string | number) => {
        const region = String(regionValue);
        setSelectedRegion(region);
        fetchEvents(region);
    };

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
            <div className="mb-3">
                <RadioGroup
                    name="orisRegion"
                    options={ORIS_REGION_KEYS.map(k => ({value: k, label: labels.orisRegions[k]}))}
                    value={selectedRegion}
                    onChange={handleRegionChange}
                    direction="horizontal"
                    disabled={isSubmitting}
                />
            </div>

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
                            {event.date} {event.organizer && `${event.organizer} `}{event.name}{event.location && ` — ${event.location.length > 15 ? event.location.substring(0, 15) + '…' : event.location}`}
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
