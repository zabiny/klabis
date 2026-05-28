import {useCallback, useEffect, useRef, useState} from 'react';
import {type BulkImportResult, type OrisEvent, ORIS_REGION_KEYS} from '../api/orisEvents';
import type {HalFormsTemplate} from '../api/types';
import {labels} from '../localization';
import {useAuthorizedMutation, useAuthorizedQuery} from './useAuthorizedFetch';
import {useFormCacheInvalidation} from './useFormCacheInvalidation';

export type OrisImportFetchState = 'loading' | 'success' | 'error';

export interface UseOrisEventImportOptions {
    onImported?: () => void;
}

export interface UseOrisEventImportResult {
    events: OrisEvent[];
    fetchState: OrisImportFetchState;
    selectedRegion: string;
    isSubmitting: boolean;
    submitError: string | null;
    onRegionChange: (region: string) => void;
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

const DEFAULT_SELECTION_LIMIT = 50;

function deriveLimit(template: HalFormsTemplate | undefined): number {
    if (!template?.properties) return DEFAULT_SELECTION_LIMIT;
    const multiProp = template.properties.find((p) => p.multi === true || p.multiple === true);
    if (!multiProp || multiProp.max === undefined) return DEFAULT_SELECTION_LIMIT;
    return multiProp.max;
}

function deriveBodyKey(template: HalFormsTemplate | undefined): string {
    if (!template?.properties) return 'orisIds';
    const multiProp = template.properties.find((p) => p.multi === true || p.multiple === true);
    return multiProp?.name ?? 'orisIds';
}

export function useOrisEventImport(
    template: HalFormsTemplate | undefined,
    isOpen: boolean,
    options: UseOrisEventImportOptions = {},
): UseOrisEventImportResult {
    const [selectedRegion, setSelectedRegion] = useState<string>(ORIS_REGION_KEYS[0]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
    const [importResult, setImportResult] = useState<BulkImportResult | null>(null);
    const onImportedRef = useRef(options.onImported);
    onImportedRef.current = options.onImported;
    const {invalidateAllCaches} = useFormCacheInvalidation();
    const invalidateAllCachesRef = useRef(invalidateAllCaches);
    invalidateAllCachesRef.current = invalidateAllCaches;

    const batchImportHref = template?.target;
    const submitMethod = template?.method ?? 'POST';
    const selectionLimit = deriveLimit(template);

    useEffect(() => {
        if (isOpen) {
            setSelectedRegion(ORIS_REGION_KEYS[0]);
            setSelectedIds(new Set());
            setSubmitError(null);
            setImportResult(null);
        }
    }, [isOpen]);

    const eventsUrl = isOpen && batchImportHref
        ? `/api/oris/events?region=${encodeURIComponent(selectedRegion)}`
        : '';

    const {data, isError, isSuccess} = useAuthorizedQuery<OrisEvent[]>(eventsUrl, {
        enabled: isOpen && !!batchImportHref,
    });

    const fetchState: OrisImportFetchState = isError ? 'error' : isSuccess ? 'success' : 'loading';

    const events = data ?? [];

    const isSelectionLimitReached = selectedIds.size >= selectionLimit;
    const isAllSelected = events.length > 0 && selectedIds.size === events.length;
    const isSomeSelected = selectedIds.size > 0 && !isAllSelected;
    const canSubmit = fetchState === 'success' && events.length > 0 && selectedIds.size > 0 && !isSubmitting;

    const {mutate} = useAuthorizedMutation({method: submitMethod});

    const onRegionChange = useCallback((region: string) => {
        setSelectedRegion(region);
        setSelectedIds(new Set());
    }, []);

    const onToggleId = useCallback((id: number) => {
        setSelectedIds((prev) => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            } else {
                if (next.size >= selectionLimit) {
                    return prev;
                }
                next.add(id);
            }
            return next;
        });
    }, [selectionLimit]);

    const onToggleAll = useCallback(() => {
        setSelectedIds((prev) => {
            const allSelected = prev.size === events.length && events.length > 0;
            const limitReached = prev.size >= selectionLimit;
            if (allSelected || limitReached) {
                return new Set();
            }
            return new Set(events.slice(0, selectionLimit).map((e) => e.id));
        });
    }, [events, selectionLimit]);

    const onImportBatch = useCallback(() => {
        if (selectedIds.size === 0 || !batchImportHref) return;

        setIsSubmitting(true);
        setSubmitError(null);

        const bodyKey = deriveBodyKey(template);

        mutate(
            {url: batchImportHref, data: {[bodyKey]: Array.from(selectedIds)}},
            {
                onSuccess: async ({data: responseData}) => {
                    setIsSubmitting(false);
                    const result = responseData as BulkImportResult;
                    setImportResult(result);
                    await invalidateAllCachesRef.current();
                    onImportedRef.current?.();
                },
                onError: () => {
                    setIsSubmitting(false);
                    setSubmitError(labels.orisImport.importFailed);
                },
            },
        );
    }, [selectedIds, batchImportHref, template, mutate]);

    return {
        events,
        fetchState,
        selectedRegion,
        isSubmitting,
        submitError,
        onRegionChange,
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
    };
}
