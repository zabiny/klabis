import {Modal} from '../UI/Modal';
import {Button} from '../UI/Button';
import {Spinner} from '../UI/Spinner';
import {RadioGroup} from '../UI/forms';
import {labels} from '../../localization';
import {type OrisEvent, ORIS_REGION_KEYS, type BulkImportResult} from '../../api/orisEvents';
import type {OrisImportFetchState} from '../../hooks/useOrisEventImport';
import {CalendarDays, CheckCircle, XCircle} from 'lucide-react';

export interface ImportOrisEventModalProps {
    isOpen: boolean;
    onClose: () => void;
    events: OrisEvent[];
    fetchState: OrisImportFetchState;
    selectedRegion: string;
    onRegionChange: (region: string) => void;
    isSubmitting: boolean;
    submitError: string | null;
    selectedIds: Set<number>;
    onToggleId: (id: number) => void;
    onToggleAll: () => void;
    onImportBatch: () => void;
    importResult: BulkImportResult | null;
    isAllSelected: boolean;
    isSomeSelected: boolean;
    canSubmit: boolean;
    selectionLimit: number;
    isSelectionLimitReached: boolean;
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
    selectedIds,
    onToggleId,
    onToggleAll,
    onImportBatch,
    importResult,
    isAllSelected,
    isSomeSelected,
    canSubmit,
    selectionLimit,
    isSelectionLimitReached,
}: ImportOrisEventModalProps) => {
    const selectedCount = selectedIds.size;

    if (importResult) {
        return (
            <Modal
                isOpen={isOpen}
                onClose={onClose}
                title={labels.orisImport.resultTitle}
                size="lg"
                footer={
                    <Button variant="secondary" onClick={onClose}>
                        {labels.orisImport.done}
                    </Button>
                }
            >
                <div className="flex flex-col gap-4">
                    <p className="text-text-primary font-medium">
                        {labels.orisImport.resultSummary(importResult.successCount, importResult.totalProcessed)}
                    </p>

                    <ul className="flex flex-col gap-2 max-h-80 overflow-y-auto">
                        {importResult.results.map((item) => (
                            <li key={item.orisId} className="flex items-start gap-3 py-1.5 border-b border-border last:border-0">
                                {item.status === 'IMPORTED' ? (
                                    <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                                ) : (
                                    <XCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                                )}
                                <div className="flex flex-col gap-0.5 min-w-0">
                                    <span className={`font-medium text-sm ${item.status === 'FAILED' ? 'text-red-700' : 'text-text-primary'}`}>
                                        {item.name ?? `ORIS #${item.orisId}`}
                                    </span>
                                    {item.date && (
                                        <span className="text-xs text-text-secondary">{item.date}</span>
                                    )}
                                    {item.error && (
                                        <span className="text-xs text-text-secondary">{item.error}</span>
                                    )}
                                </div>
                            </li>
                        ))}
                    </ul>
                </div>
            </Modal>
        );
    }

    const footer = (
        <div className="flex items-center justify-between w-full">
            <div className="flex items-center gap-2">
                <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
                    {labels.buttons.cancel}
                </Button>
                <Button onClick={onImportBatch} disabled={!canSubmit} loading={isSubmitting}>
                    {isSubmitting
                        ? labels.buttons.importing
                        : labels.orisImport.importSelected(selectedCount)}
                </Button>
            </div>
        </div>
    );

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title={labels.orisImport.dialogTitle}
            footer={footer}
            closeOnBackdropClick={!isSubmitting}
            size="lg"
        >
            <div className="mb-3">
                <RadioGroup
                    name="orisRegion"
                    options={ORIS_REGION_KEYS.map(k => ({value: k, label: labels.orisRegions[k]}))}
                    value={selectedRegion}
                    onChange={(v) => onRegionChange(String(v))}
                    direction="horizontal"
                    disabled={isSubmitting}
                />
            </div>

            {fetchState === 'loading' && (
                <div className="flex flex-col items-center gap-3 py-8">
                    <Spinner size="lg" />
                    <p className="text-text-secondary">{labels.orisImport.loadingMessage}</p>
                </div>
            )}

            {fetchState === 'error' && (
                <p className="text-error">{labels.errors.importOrisLoadFailed}</p>
            )}

            {fetchState === 'success' && events.length === 0 && (
                <div className="flex flex-col items-center gap-2 py-8 text-center">
                    <p className="font-medium text-text-primary">{labels.orisImport.emptyHeading}</p>
                    <p className="text-sm text-text-secondary">{labels.orisImport.emptyHint}</p>
                </div>
            )}

            {fetchState === 'success' && events.length > 0 && (
                <div className="flex flex-col gap-2">
                    <div className="flex items-center gap-2 py-1.5 border-b border-border">
                        <input
                            type="checkbox"
                            id="select-all-events"
                            aria-label={labels.orisImport.selectAll}
                            checked={isAllSelected}
                            ref={(el) => {
                                if (el) el.indeterminate = isSomeSelected;
                            }}
                            onChange={onToggleAll}
                            className="w-4 h-4 text-primary border-border rounded focus:ring-primary"
                            disabled={isSubmitting}
                        />
                        <label htmlFor="select-all-events" className="text-sm font-medium text-text-primary cursor-pointer select-none">
                            {labels.orisImport.selectAll}
                        </label>
                        {events.length > 10 && (
                            <span className="ml-auto text-xs text-text-secondary">
                                {labels.orisImport.shownOf(events.length, events.length)}
                            </span>
                        )}
                    </div>

                    {isSelectionLimitReached && (
                        <p className="text-xs text-text-secondary px-1">
                            {labels.orisImport.limitReachedHint(selectionLimit)}
                        </p>
                    )}

                    <ul className="flex flex-col max-h-96 overflow-y-auto divide-y divide-border">
                        {events.map((event) => {
                            const isSelected = selectedIds.has(event.id);
                            const isCheckboxDisabled = isSubmitting || (!isSelected && isSelectionLimitReached);
                            return (
                                <li
                                    key={event.id}
                                    data-event-id={event.id}
                                    className={`flex items-start gap-3 py-2 px-1 rounded cursor-pointer transition-colors
                                        ${isSelected ? 'bg-primary/10 border border-primary' : 'hover:bg-surface-raised'}`}
                                    onClick={() => !isCheckboxDisabled && onToggleId(event.id)}
                                >
                                    <input
                                        type="checkbox"
                                        aria-label={event.name}
                                        value={String(event.id)}
                                        checked={isSelected}
                                        onChange={() => onToggleId(event.id)}
                                        onClick={(e) => e.stopPropagation()}
                                        className="w-4 h-4 text-primary border-border rounded focus:ring-primary mt-0.5 flex-shrink-0"
                                        disabled={isCheckboxDisabled}
                                    />
                                    <div className="flex flex-col gap-0.5 min-w-0">
                                        <span className="font-medium text-sm text-text-primary truncate">{event.name}</span>
                                        <span className="text-xs text-text-secondary flex items-center gap-1">
                                            <CalendarDays className="w-3 h-3 flex-shrink-0" />
                                            {event.date}
                                            {event.organizer && ` · ${event.organizer}`}
                                            {event.location && ` — ${event.location}`}
                                        </span>
                                    </div>
                                </li>
                            );
                        })}
                    </ul>
                </div>
            )}

            {submitError && (
                <p className="mt-3 text-error">{submitError}</p>
            )}
        </Modal>
    );
};
