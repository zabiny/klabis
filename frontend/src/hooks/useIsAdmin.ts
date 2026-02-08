import {useMemo} from 'react';
import {useRootNavigation} from './useRootNavigation';

/**
 * Hook to detect if the current user is a System Admin
 *
 * Admin status is determined by the presence of a 'systemEvents' link
 * in the root navigation. Only System Admin users will have this link
 * exposed by the backend.
 *
 * @returns {Object} with isAdmin boolean and isLoading state
 */
export function useIsAdmin() {
    const {data: navigationItems = [], isLoading} = useRootNavigation();

    const isAdmin = useMemo(() => {
        if (!navigationItems || navigationItems.length === 0) {
            return false;
        }
        return navigationItems.some((item) => item.rel === 'sourceEvents');
    }, [navigationItems]);

    return {
        isAdmin,
        isLoading,
    };
}
