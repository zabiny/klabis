/**
 * Custom hook for fetching HAL Forms data from target endpoints.
 *
 * Flow: OPTIONS probe → (if GET allowed) GET prefill request.
 *
 * First probes the target URL via OPTIONS to check whether GET is supported.
 * Only if the Allow header includes GET does this hook issue the actual GET
 * request for form pre-fill data. This avoids speculative GETs on write-only
 * endpoints and removes the need to swallow 404/405 errors.
 *
 * Query Key Convention: ['hal-form-data', targetUrl]
 * GET Cache Time: 1 minute (forms often have related data that changes together)
 * GET Stale Time: 0 (always fetch fresh form data on mount)
 * OPTIONS Cache / Stale Time: 10 minutes (assumed stable per server config lifecycle)
 */

import {useMemo} from 'react';
import type {HalFormsTemplate} from '../api';
import {normalizeKlabisApiPath, shouldFetchTargetData} from '../utils/halFormsUtils';
import {useAuthorizedQuery} from "./useAuthorizedFetch.ts";
import {useHalFormGetAvailability} from "./useHalFormGetAvailability.ts";

/**
 * Result object returned by useHalFormData hook
 */
export interface UseHalFormDataReturn {
    /**
     * Form data to pre-fill form fields. Null while loading or if fetch disabled.
     * Returns current resource data if not fetching from target.
     */
    formData: Record<string, unknown> | null;

    /**
     * True while the OPTIONS probe OR the GET prefill request is in flight.
     */
    isLoadingTargetData: boolean;

    /**
     * Error from the OPTIONS probe or the GET request, if applicable.
     * OPTIONS error is preferred since it gates the GET. Null when no error.
     */
    targetFetchError: Error | null;

    /**
     * Manual refetch function for the GET prefill data.
     * The OPTIONS result is considered stable (cached 10 minutes) and is not re-probed.
     */
    refetchTargetData: () => void;
}

/**
 * Hook to manage form data fetching from target endpoints.
 *
 * When a form template's target differs from the current resource, this hook:
 * 1. Probes the target URL via OPTIONS to check if GET is supported.
 * 2. If GET is allowed, fetches prefill data from that target.
 * 3. If GET is not allowed, returns current resource data with no error.
 * 4. If the OPTIONS probe fails, surfaces the error through targetFetchError.
 *
 * When the target equals the current resource path, returns the current resource
 * data immediately without any network requests.
 *
 * @param selectedTemplate - The currently selected HAL Forms template (null if no template selected)
 * @param currentResourceData - The current resource data to use as fallback
 * @param currentPathname - The current resource pathname to compare with template target
 * @returns Form state with data, loading flag, error, and refetch function
 *
 * @example
 * // When form template target differs from current resource
 * const { formData, isLoadingTargetData, targetFetchError } = useHalFormData(
 *     template,
 *     resourceData,
 *     '/api/members/123'
 * );
 *
 * if (isLoadingTargetData) return <Spinner />;
 * if (targetFetchError) return <Alert variant="error">{targetFetchError.message}</Alert>;
 *
 * return <Form initialValues={formData} />;
 *
 * @throws Does not throw. Errors are returned in targetFetchError field.
 */
export function useHalFormData(
    selectedTemplate: HalFormsTemplate | null,
    currentResourceData: Record<string, unknown>,
    currentPathname: string
): UseHalFormDataReturn {
    // Validate template structure
    if (selectedTemplate && !selectedTemplate.properties) {
        console.error('HalFormsTemplate missing required properties field', selectedTemplate);
    }

    // Determine if we need to fetch from target
    const shouldFetch = useMemo(() => {
        if (!selectedTemplate) return false;
        return shouldFetchTargetData(selectedTemplate.target, currentPathname);
    }, [selectedTemplate, currentPathname]);

    const targetUrl = selectedTemplate?.target && normalizeKlabisApiPath(selectedTemplate?.target);

    // Phase 1: Probe the target via OPTIONS to determine whether GET is supported.
    // Only fires when shouldFetch is true so we don't probe same-resource targets.
    const probeResult = useHalFormGetAvailability(targetUrl || undefined, shouldFetch);

    // Phase 2: Fetch prefill data only when the OPTIONS probe confirmed GET is supported.
    // Stale Time: 0 — always fetch fresh form data on mount
    // Cache Time (gcTime): 1 minute — forms often have related data that changes together
    // Retry: false — GET errors after a positive probe are genuine failures
    const {
        data: fetchedData,
        isLoading: isGetLoading,
        error: getError,
        refetch
    } = useAuthorizedQuery<Record<string, unknown>>(targetUrl || '', {
        enabled: shouldFetch && probeResult.isGetAllowed === true,
        staleTime: 0,
        gcTime: 60000,
        retry: false
    });

    const formData = fetchedData ? fetchedData : currentResourceData ?? null;

    // OPTIONS error takes precedence since it gates the entire prefill flow.
    const targetFetchError = (probeResult.error ?? getError) as Error | null;

    return {
        formData,
        isLoadingTargetData: probeResult.isLoading || isGetLoading,
        targetFetchError,
        refetchTargetData: async () => {
            if (shouldFetch) {
                await refetch();
            }
        },
    };
}
