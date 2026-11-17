import {useState} from 'react';
import type {ReactElement} from 'react';
import {CalendarDays, CheckCircle, XCircle} from 'lucide-react';
import {Button} from '../UI/Button';
import {Modal} from '../UI/Modal';
import {Spinner} from '../UI/Spinner';
import {RadioGroup} from '../UI/forms';
import {labels} from '../../localization';
import {halResource} from '../../api/halResource';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {type BulkImportResult, type BulkImportResultItem, type OrisEvent, ORIS_REGION_KEYS} from '../../api/orisEvents';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {isMultipleProperty} from '../HalNavigator2/halforms/utils';

const DEFAULT_SELECTION_LIMIT = 50;

type FeedState = 'loading' | 'success' | 'error';

interface ImportFlow {
    isOpen: boolean;
    open: () => void;
    close: () => void;
    events: OrisEvent[];
    feedState: FeedState;
    selectedRegion: string;
    changeRegion: (region: string) => void;
    isSubmitting: boolean;
    submitError: string | null;
    selectedIds: ReadonlySet<number>;
    toggleId: (id: number) => void;
    toggleAll: () => void;
    submit: () => void;
    result: BulkImportResult | null;
    isAllSelected: boolean;
    isSomeSelected: boolean;
    canSubmit: boolean;
    selectionLimit: number;
    isSelectionLimitReached: boolean;
}

interface ImportOrisEventsButtonProps {
    resource: HalResponse;
}

function useImportFlow(template: HalFormsTemplate | undefined): ImportFlow {
    const [isOpen, setIsOpen] = useState(false);
    const [selectedRegion, setSelectedRegion] = useState<string>(ORIS_REGION_KEYS[0]);
    const [selectedIds, setSelectedIds] = useState<ReadonlySet<number>>(() => new Set<number>());
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const [result, setResult] = useState<BulkImportResult | null>(null);

    const importTarget = template?.target;
    const submitMethod = template?.method ?? 'POST';
    const multiProperty = template?.properties.find(isMultipleProperty);
    const selectionLimit = multiProperty?.max ?? DEFAULT_SELECTION_LIMIT;
    const bodyKey = multiProperty?.name ?? 'orisIds';

    const eventsUrl = isOpen && importTarget
        ? `/api/oris/events?region=${encodeURIComponent(selectedRegion)}`
        : undefined;

    const {data, isError, isSuccess} = useAuthorizedQuery<OrisEvent[]>(eventsUrl);
    const {mutate} = useAuthorizedMutation({method: submitMethod});

    const events = data ?? [];
    const feedState: FeedState = isError ? 'error' : isSuccess ? 'success' : 'loading';
    const isSelectionLimitReached = selectedIds.size >= selectionLimit;
    const isAllSelected = events.length > 0 && selectedIds.size === events.length;
    const isSomeSelected = selectedIds.size > 0 && !isAllSelected;
    const canSubmit = feedState === 'success' && events.length > 0 && selectedIds.size > 0 && !isSubmitting;

    const open = () => {
        setSelectedRegion(ORIS_REGION_KEYS[0]);
        setSelectedIds(new Set<number>());
        setSubmitError(null);
        setResult(null);
        setIsOpen(true);
    };

    const close = () => setIsOpen(false);

    const changeRegion = (region: string) => {
        setSelectedRegion(region);
        setSelectedIds(new Set<number>());
    };

    const toggleId = (id: number) => {
        setSelectedIds((previous) => {
            const next = new Set(previous);
            if (next.delete(id)) {
                return next;
            }
            if (next.size >= selectionLimit) {
                return previous;
            }
            next.add(id);
            return next;
        });
    };

    const toggleAll = () => {
        setSelectedIds((previous) => {
            const allSelected = events.length > 0 && previous.size === events.length;
            if (allSelected || previous.size >= selectionLimit) {
                return new Set<number>();
            }
            return new Set(events.slice(0, selectionLimit).map((event) => event.id));
        });
    };

    const submit = () => {
        if (selectedIds.size === 0 || !importTarget) {
            return;
        }

        setIsSubmitting(true);
        setSubmitError(null);

        mutate(
            {url: importTarget, data: {[bodyKey]: Array.from(selectedIds)}},
            {
                onSuccess: ({data: payload}) => {
                    setIsSubmitting(false);
                    const rawResult = payload as BulkImportResult;
                    setResult({
                        ...rawResult,
                        results: rawResult.results.map((item) => ({
                            ...item,
                            status: String(item.status).toUpperCase() as BulkImportResultItem['status'],
                        })),
                    });
                },
                onError: () => {
                    setIsSubmitting(false);
                    setSubmitError(labels.orisImport.importFailed);
                },
            },
        );
    };

    return {
        isOpen,
        open,
        close,
        events,
        feedState,
        selectedRegion,
        changeRegion,
        isSubmitting,
        submitError,
        selectedIds,
        toggleId,
        toggleAll,
        submit,
        result,
        isAllSelected,
        isSomeSelected,
        canSubmit,
        selectionLimit,
        isSelectionLimitReached,
    };
}

const ImportSelectionDialog = ({flow}: {flow: ImportFlow}): ReactElement => {
    const selectedCount = flow.selectedIds.size;

    const footer = (
        <div className="flex items-center justify-between w-full">
            <div className="flex items-center gap-2">
                <Button variant="secondary" onClick={flow.close} disabled={flow.isSubmitting}>
                    {labels.buttons.cancel}
                </Button>
                <Button onClick={flow.submit} disabled={!flow.canSubmit} loading={flow.isSubmitting}>
                    {flow.isSubmitting
                        ? labels.buttons.importing
                        : labels.orisImport.importSelected(selectedCount)}
                </Button>
            </div>
        </div>
    );

    return (
        <Modal
            isOpen
            onClose={flow.close}
            title={labels.orisImport.dialogTitle}
            footer={footer}
            closeOnBackdropClick={!flow.isSubmitting}
            size="lg"
        >
            <div className="mb-3">
                <RadioGroup
                    name="orisRegion"
                    options={ORIS_REGION_KEYS.map(key => ({value: key, label: labels.orisRegions[key]}))}
                    value={flow.selectedRegion}
                    onChange={(value) => flow.changeRegion(String(value))}
                    direction="horizontal"
                    disabled={flow.isSubmitting}
                />
            </div>

            {flow.feedState === 'loading' && (
                <div className="flex flex-col items-center gap-3 py-8">
                    <Spinner size="lg"/>
                    <p className="text-text-secondary">{labels.orisImport.loadingMessage}</p>
                </div>
            )}

            {flow.feedState === 'error' && (
                <p className="text-error">{labels.errors.importOrisLoadFailed}</p>
            )}

            {flow.feedState === 'success' && flow.events.length === 0 && (
                <div className="flex flex-col items-center gap-2 py-8 text-center">
                    <p className="font-medium text-text-primary">{labels.orisImport.emptyHeading}</p>
                    <p className="text-sm text-text-secondary">{labels.orisImport.emptyHint}</p>
                </div>
            )}

            {flow.feedState === 'success' && flow.events.length > 0 && (
                <div className="flex flex-col gap-2">
                    <div className="flex items-center gap-2 py-1.5 border-b border-border">
                        <input
                            type="checkbox"
                            id="select-all-events"
                            aria-label={labels.orisImport.selectAll}
                            checked={flow.isAllSelected}
                            ref={(element) => {
                                if (element) element.indeterminate = flow.isSomeSelected;
                            }}
                            onChange={flow.toggleAll}
                            className="w-4 h-4 text-primary border-border rounded focus:ring-primary"
                            disabled={flow.isSubmitting}
                        />
                        <label htmlFor="select-all-events" className="text-sm font-medium text-text-primary cursor-pointer select-none">
                            {labels.orisImport.selectAll}
                        </label>
                        {flow.events.length > 10 && (
                            <span className="ml-auto text-xs text-text-secondary">
                                {labels.orisImport.shownOf(flow.events.length, flow.events.length)}
                            </span>
                        )}
                    </div>

                    {flow.isSelectionLimitReached && (
                        <p className="text-xs text-text-secondary px-1">
                            {labels.orisImport.limitReachedHint(flow.selectionLimit)}
                        </p>
                    )}

                    <ul className="flex flex-col max-h-96 overflow-y-auto divide-y divide-border">
                        {flow.events.map((event) => {
                            const isSelected = flow.selectedIds.has(event.id);
                            const isCheckboxDisabled = flow.isSubmitting || (!isSelected && flow.isSelectionLimitReached);
                            return (
                                <li
                                    key={event.id}
                                    data-event-id={event.id}
                                    className={`flex items-start gap-3 py-2 px-1 rounded cursor-pointer transition-colors
                                        ${isSelected ? 'bg-primary/10 border border-primary' : 'hover:bg-surface-raised'}`}
                                    onClick={() => !isCheckboxDisabled && flow.toggleId(event.id)}
                                >
                                    <input
                                        type="checkbox"
                                        aria-label={event.name}
                                        value={String(event.id)}
                                        checked={isSelected}
                                        onChange={() => flow.toggleId(event.id)}
                                        onClick={(e) => e.stopPropagation()}
                                        className="w-4 h-4 text-primary border-border rounded focus:ring-primary mt-0.5 flex-shrink-0"
                                        disabled={isCheckboxDisabled}
                                    />
                                    <div className="flex flex-col gap-0.5 min-w-0">
                                        <span className="font-medium text-sm text-text-primary truncate">{event.name}</span>
                                        <span className="text-xs text-text-secondary flex items-center gap-1">
                                            <CalendarDays className="w-3 h-3 flex-shrink-0"/>
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

            {flow.submitError && (
                <p className="mt-3 text-error">{flow.submitError}</p>
            )}
        </Modal>
    );
};

const ImportResultDialog = ({flow}: {flow: ImportFlow}): ReactElement | null => {
    const result = flow.result;

    if (!result) {
        return null;
    }

    return (
        <Modal
            isOpen
            onClose={flow.close}
            title={labels.orisImport.resultTitle}
            size="lg"
            footer={
                <Button variant="secondary" onClick={flow.close}>
                    {labels.orisImport.done}
                </Button>
            }
        >
            <div className="flex flex-col gap-4">
                <p className="text-text-primary font-medium">
                    {labels.orisImport.resultSummary(result.successCount, result.totalProcessed)}
                </p>

                <ul className="flex flex-col gap-2 max-h-80 overflow-y-auto">
                    {result.results.map((item) => (
                        <li key={item.orisId} className="flex items-start gap-3 py-1.5 border-b border-border last:border-0">
                            {item.status === 'IMPORTED' ? (
                                <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5"/>
                            ) : (
                                <XCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5"/>
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
};

export function ImportOrisEventsButton({resource}: ImportOrisEventsButtonProps): ReactElement | null {
    const actions = halResource(resource);
    const importTemplate = actions.template('importEventsBatch')?.template ?? actions.template('importEvent')?.template;
    const flow = useImportFlow(importTemplate);

    if (!importTemplate) {
        return null;
    }

    return (
        <>
            <Button variant="secondary" onClick={flow.open}>
                {labels.templates.importEvent}
            </Button>
            {flow.isOpen && (flow.result
                ? <ImportResultDialog flow={flow}/>
                : <ImportSelectionDialog flow={flow}/>)}
        </>
    );
}
