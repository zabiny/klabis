/**
 * Custom hook for fetching HAL Forms data from target endpoints
 * Automatically fetches data from the template's target if it differs from the current resource
 */

import {useMemo} from 'react';
import {useQuery} from '@tanstack/react-query';
import type {HalFormsTemplate} from '../api';
import {fetchResource} from '../components/HalNavigator/hooks';
import {normalizeApiPath, shouldFetchTargetData} from '../utils/halFormsUtils';

export interface UseHalFormDataReturn {
    formData: Record<string, unknown> | null;
    isLoadingTargetData: boolean;
    targetFetchError: Error | null;
    refetchTargetData: () => void;
}

/**
 * Hook to manage form data fetching from target endpoints
 *
 * @param selectedTemplate - The currently selected HAL Forms template
 * @param currentResourceData - The current resource data
 * @param currentPathname - The current resource pathname
 * @returns Form data, loading state, error state, and refetch function
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
    const {
        data: targetData,
        isLoading,
        error,
        refetch,
    } = useQuery({
        queryKey: ['hal-form-data', targetUrl],
        queryFn: () => fetchResource(targetUrl!),
        enabled: !!targetUrl,
        staleTime: 0, // Always fetch fresh data
        gcTime: 60000, // Clean up cache after 1 minute
        retry: false, // Don't auto-retry on error
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
