import {useCallback, useEffect, useRef, useState} from 'react';
import {type BulkImportResult, type OrisEvent, ORIS_REGION_KEYS} from '../api/orisEvents';
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
}

export function useOrisEventImport(
    batchImportHref: string,
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

    const {mutate} = useAuthorizedMutation({method: 'POST'});

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
                next.add(id);
            }
            return next;
        });
    }, []);

    const onToggleAll = useCallback(() => {
        setSelectedIds((prev) => {
            const allSelected = prev.size === events.length && events.length > 0;
            if (allSelected) {
                return new Set();
            }
            return new Set(events.map((e) => e.id));
        });
    }, [events]);

    const onImportBatch = useCallback(() => {
        if (selectedIds.size === 0 || !batchImportHref) return;

        setIsSubmitting(true);
        setSubmitError(null);

        mutate(
            {url: batchImportHref, data: {orisIds: Array.from(selectedIds)}},
            {
                onSuccess: async ({data: responseData}) => {
                    setIsSubmitting(false);
                    const result = responseData as BulkImportResult;
                    setImportResult(result);
                    await invalidateAllCaches();
                    onImportedRef.current?.();
                },
                onError: () => {
                    setIsSubmitting(false);
                    setSubmitError(labels.orisImport.importFailed);
                },
            },
        );
    }, [selectedIds, batchImportHref, mutate, invalidateAllCaches]);

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
    };
}
