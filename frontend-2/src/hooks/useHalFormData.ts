/**
 * Custom hook for fetching HAL Forms data from target endpoints
 *
 * Automatically fetches data from the template's target if it differs from the current resource.
 * Uses React Query for caching and request deduplication. Returns a custom interface for
 * convenient form-specific state management.
 *
 * Query Key Convention: ['hal-form-data', targetUrl]
 * Cache Time: 1 minute (forms often have related data that changes together)
 * Stale Time: 0 (always fetch fresh form data on mount)
 */

import {useMemo} from 'react';
import type {HalFormsTemplate} from '../api';
import {normalizeKlabisApiPath, shouldFetchTargetData} from '../utils/halFormsUtils';
import {useAuthorizedQuery} from "./useAuthorizedFetch.ts";
import {FetchError} from "../api/authorizedFetch.ts";

/**
 * Check if error is a fetch error with specific HTTP status
 * @internal
 */
function isFetchErrorWithStatus(error: unknown, statuses: number[]): boolean {
    if (error && error instanceof FetchError) {
        const status = (error as FetchError).responseStatus;
        return statuses.includes(status);
    }
    return false;
}

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
     * True while fetching target form data (only if shouldFetch is true)
     */
    isLoadingTargetData: boolean;

    /**
     * Error from target fetch, if applicable (only if shouldFetch is true)
     */
    targetFetchError: Error | null;

    /**
     * Manual refetch function for target data
     */
    refetchTargetData: () => void;
}

/**
 * Hook to manage form data fetching from target endpoints
 *
 * When a form template's target differs from the current resource, this hook will fetch
 * data from that target using React Query. If the target is the same as the current resource,
 * it returns the current resource data immediately.
 *
 * Handles common HTTP errors (404, 405) by returning empty data, allowing forms to display
 * with blank fields. Other errors are propagated through the error state.
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

    // Fetch data from target using React Query
    // Stale Time: 0 - Always fetch fresh form data on mount
    // Cache Time (gcTime): 1 minute - Forms often have related data that changes together
    // Retry: false - Don't auto-retry; let the form display with empty values on 404/405 (or with error on other statuses)
    const {
        data: fetchedData,
        isLoading,
        error,
        refetch
    } = useAuthorizedQuery<Record<string, unknown>>(targetUrl || '', {
        enabled: !!targetUrl,
        staleTime: 0,
        gcTime: 60000,
        retry: false
    })

    const formData = fetchedData ? fetchedData : currentResourceData ?? null;

    const filteredError = isErrorForUndefinedGetMethod(error) ? null : error;

    return {
        formData,
        isLoadingTargetData: isLoading,
        targetFetchError: filteredError,
        refetchTargetData: async () => {
            if (shouldFetch) {
                await refetch();
            }
        },
    };
}


function isErrorForUndefinedGetMethod(error: unknown): boolean {
    // Hal+Forms POST/PUT/DELETE endpoints doesn't need GET endpoint - if GET endpoint is not defined, form shall be initialized with empty data.
    return isFetchErrorWithStatus(error, [404, 405]);
}