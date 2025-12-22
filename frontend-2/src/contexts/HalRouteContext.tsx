import React, {useContext} from 'react';
import {useLocation} from 'react-router-dom';
import {useQuery} from '@tanstack/react-query';
import {fetchResource} from '../components/HalNavigator/hooks';
import type {HalResponse} from '../api';

/**
 * Context value provided by HalRouteProvider
 * Contains HAL resource data fetched from /api + current location pathname
 */
export interface HalRouteContextValue {
    /** Fetched HAL resource data from /api + pathname */
    resourceData: HalResponse | null;

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
}

const HalRouteContext = React.createContext<HalRouteContextValue | null>(null);

interface HalRouteProviderProps {
    children: React.ReactNode;
}

/**
 * Provider component that wraps the entire app
 * Automatically fetches HAL resource data when route pathname changes
 * Uses React Query for caching, deduplication, and advanced features
 */
export const HalRouteProvider: React.FC<HalRouteProviderProps> = ({children}) => {
    const location = useLocation();

    // Skip HAL fetching for login route
    const shouldFetch = !location.pathname.startsWith('/login');

    // React Query hook for fetching HAL data
    // Cache key is the API URL so each pathname has its own cached data
    const {data, isLoading, error, refetch, status} = useQuery({
        queryKey: ['/api' + location.pathname],
        queryFn: async () => {
            return fetchResource('/api' + location.pathname);
        },
        enabled: shouldFetch,
        staleTime: 5 * 60 * 1000, // 5 minutes
        retry: 1,
    });

    // Convert React Query status to context queryState
    const queryState: 'idle' | 'pending' | 'success' | 'error' =
        status === 'pending' ? 'pending' :
            status === 'error' ? 'error' :
                status === 'success' ? 'success' :
                    'idle';

    const contextValue: HalRouteContextValue = {
        resourceData: data ?? null,
        isLoading,
        error: error instanceof Error ? error : null,
        refetch: async () => {
            await refetch();
        },
        pathname: location.pathname,
        queryState,
    };

    return (
        <HalRouteContext.Provider value={contextValue}>
            {children}
        </HalRouteContext.Provider>
    );
};

/**
 * Hook to access HAL resource data from context
 * Must be used within a component wrapped by HalRouteProvider
 *
 * @example
 * const { resourceData, isLoading, error } = useHalRoute();
 */
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
