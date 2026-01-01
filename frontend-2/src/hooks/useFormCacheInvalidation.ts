/**
 * Hook for invalidating ReactQuery cache after form submission
 * Invalidates all queries with 'authorized' query key to ensure data consistency
 */

import {useQueryClient} from '@tanstack/react-query';

/**
 * Hook that provides a function to invalidate all cached queries
 * Typically called after successful form submission to ensure fresh data
 */
export function useFormCacheInvalidation() {
    const queryClient = useQueryClient();

    /**
     * Invalidates all cached queries that start with the 'authorized' query key
     * This ensures all HAL API data is refreshed after mutations that could affect
     * permissions, relationships, or any other cached data
     */
    const invalidateAllCaches = async () => {
        await queryClient.invalidateQueries({
            queryKey: ['authorized'],
        });
    };

    return {invalidateAllCaches};
}
