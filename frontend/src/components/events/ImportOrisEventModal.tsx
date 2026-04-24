import {useState} from 'react';
import {Modal} from '../UI/Modal';
import {Button} from '../UI/Button';
import {RadioGroup} from '../UI/forms';
import {labels} from '../../localization';
import {type OrisEvent, ORIS_REGION_KEYS} from '../../api/orisEvents';
import type {OrisImportFetchState} from '../../hooks/useOrisEventImport';

export interface ImportOrisEventModalProps {
    isOpen: boolean;
    onClose: () => void;
    events: OrisEvent[];
    fetchState: OrisImportFetchState;
    selectedRegion: string;
    onRegionChange: (region: string) => void;
    isSubmitting: boolean;
    submitError: string | null;
    onImport: (orisId: number) => void;
}

export const ImportOrisEventModal = ({
    isOpen,
    onClose,
    events,
    fetchState,
    selectedRegion,
    onRegionChange,
    isSubmitting,
    submitError,
    onImport,
}: ImportOrisEventModalProps) => {
    const [selectedId, setSelectedId] = useState<string>('');

    const handleRegionChange = (regionValue: string | number) => {
        setSelectedId('');
        onRegionChange(String(regionValue));
    };

    const handleSubmit = () => {
        if (!selectedId) return;
        onImport(Number(selectedId));
    };

    const isSubmitDisabled =
        fetchState !== 'success' || events.length === 0 || !selectedId || isSubmitting;

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

            {fetchState === 'success' && events.length === 0 && (
                <p className="text-text-secondary">{labels.errors.importOrisNoEvents}</p>
            )}

            {fetchState === 'success' && events.length > 0 && (
                <select
                    className="w-full rounded-md border border-border bg-surface px-3 py-2 text-text-primary focus:outline-none focus:ring-2 focus:ring-accent"
                    value={selectedId}
                    onChange={(e) => setSelectedId(e.target.value)}
                    aria-label="Vyberte závod k importu"
                >
                    <option value="">— Vyberte závod —</option>
                    {events.map((event) => (
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
