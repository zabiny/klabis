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
import {useQuery} from '@tanstack/react-query';
import type {HalFormsTemplate} from '../api';
import {fetchResource} from '../components/HalNavigator/hooks';
import {normalizeApiPath, shouldFetchTargetData} from '../utils/halFormsUtils';

/**
 * Check if error is a fetch error with specific HTTP status
 * @internal
 */
function isFetchErrorWithStatus(error: unknown, statuses: number[]): boolean {
    if (error && typeof error === 'object' && 'responseStatus' in error) {
        const status = (error as { responseStatus: number }).responseStatus;
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

    // Prepare the target URL for fetching
    const targetUrl = useMemo(() => {
        if (!selectedTemplate?.target || !shouldFetch) return null;

        const normalized = normalizeApiPath(selectedTemplate.target);
        // Ensure /api prefix
        return normalized.startsWith('/api') ? normalized : `/api${normalized}`;
    }, [selectedTemplate, shouldFetch]);

    // Fetch data from target using React Query
    // Query Key Convention: [domain, ...identifiers]
    // Stale Time: 0 - Always fetch fresh form data on mount
    // Cache Time (gcTime): 1 minute - Forms often have related data that changes together
    // Retry: false - Don't auto-retry; let the form display with empty values on 404/405
    const {
        data: targetData,
        isLoading,
        error,
        refetch,
    } = useQuery({
        queryKey: ['hal-form-data', targetUrl],
        queryFn: async () => {
            try {
                return await fetchResource(targetUrl!);
            } catch (err) {
                // If API doesn't define GET endpoint (HTTP 404 or 405), return empty data
                // This allows form to display with empty initial values on endpoint not found
                if (isFetchErrorWithStatus(err, [404, 405])) {
                    return {};
                }
                // Re-throw other errors to be handled in error state
                throw err;
            }
        },
        enabled: !!targetUrl,
        staleTime: 0,
        gcTime: 60000,
        retry: false,
    });

    // Determine which data to return
    const formData = useMemo(() => {
        if (shouldFetch) {
            // If we're fetching from target, return the target data (or null while loading/on error)
            return targetData || null;
        } else {
            // Use current resource data
            return currentResourceData;
        }
    }, [shouldFetch, targetData, currentResourceData]);

    return {
        formData,
        isLoadingTargetData: shouldFetch && isLoading,
        targetFetchError: shouldFetch && error
            ? (error instanceof Error ? error : new Error(String(error)))
            : null,
        refetchTargetData: () => {
            if (shouldFetch) {
                refetch();
            }
        },
    };
}
