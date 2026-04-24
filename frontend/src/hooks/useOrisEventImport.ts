import {useCallback, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {FetchError} from '../api/authorizedFetch';
import {type OrisEvent, ORIS_REGION_KEYS} from '../api/orisEvents';
import {labels} from '../localization';
import {extractNavigationPath} from '../utils/navigationPath';
import {useAuthorizedMutation, useAuthorizedQuery} from './useAuthorizedFetch';

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
    onImport: (orisId: number) => void;
}

export function useOrisEventImport(
    importHref: string,
    isOpen: boolean,
    options: UseOrisEventImportOptions = {},
): UseOrisEventImportResult {
    const navigate = useNavigate();
    const [selectedRegion, setSelectedRegion] = useState<string>(ORIS_REGION_KEYS[0]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    useEffect(() => {
        if (isOpen) {
            setSelectedRegion(ORIS_REGION_KEYS[0]);
        }
    }, [isOpen]);

    const eventsUrl = isOpen && importHref
        ? `/api/oris/events?region=${encodeURIComponent(selectedRegion)}`
        : '';

    const {data, isError, isSuccess} = useAuthorizedQuery<OrisEvent[]>(eventsUrl, {
        enabled: isOpen && !!importHref,
    });

    const fetchState: OrisImportFetchState = isError ? 'error' : isSuccess ? 'success' : 'loading';

    const {mutate} = useAuthorizedMutation({method: 'POST'});

    const onRegionChange = useCallback((region: string) => {
        setSelectedRegion(region);
    }, []);

    const onImport = useCallback((orisId: number) => {
        setIsSubmitting(true);
        setSubmitError(null);

        mutate(
            {url: importHref, data: {orisId}},
            {
                onSuccess: ({location}) => {
                    setIsSubmitting(false);
                    options.onImported?.();
                    if (location) {
                        navigate(extractNavigationPath(location));
                    }
                },
                onError: (error) => {
                    setIsSubmitting(false);
                    if (error instanceof FetchError && error.responseStatus === 409) {
                        setSubmitError(labels.errors.importOrisConflict);
                    } else {
                        setSubmitError(labels.errors.importOrisFailed);
                    }
                },
            },
        );
    }, [importHref, mutate, navigate, options]);

    return {
        events: data ?? [],
        fetchState,
        selectedRegion,
        isSubmitting,
        submitError,
        onRegionChange,
        onImport,
    };
}
