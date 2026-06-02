import React, {useContext} from 'react';
import type {HalResponse, Link} from '../api';

/**
 * Context value provided by HalRouteProvider
 * Contains HAL resource data fetched from API (with environment-aware prefix) + current location pathname
 */
export interface HalRouteContextValue {
    /** Fetched HAL resource data from /api + pathname */
    resourceData: HalResponse | null;

    /** Can be used to perform navigation to given resource. It will be delegated to react router with 'self' link target extracted from resource **/
    navigateToResource: (resource: HalResponse | Link, options?: { state?: unknown }) => void;

    /** Loading state while fetching from API */
    isLoading: boolean;

    /** Error state if fetch failed */
    error: Error | null;

    /** Manual refetch function for updating data after form submissions */
    refetch: () => Promise<void>;

    /** Current pathname being displayed */
    pathname: string;

    /** React Query query state */
    queryState: 'idle' | 'pending' | 'success' | 'error';

    /**
     * Returns link with given name from current resource. Only links from main resource are considered (links from subresources are not included)
     * @param linkName
     */
    getResourceLink: (linkName?: string) => Link | null;
}

export const HalRouteContext = React.createContext<HalRouteContextValue | null>(null);

export const useHalRoute = (): HalRouteContextValue => {
    const context = useContext(HalRouteContext);
    if (!context) {
        throw new Error(
            'useHalRoute must be used within a component wrapped by HalRouteProvider. ' +
            'Ensure HalRouteProvider is placed in your app hierarchy (typically in main.tsx).'
        );
    }
    return context;
};
