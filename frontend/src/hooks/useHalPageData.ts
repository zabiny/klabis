import {useHalRoute} from '../contexts/halRouteContext';
import type {HalResponse, Link} from '../api';

export interface UseHalPageDataReturn<T = HalResponse> {
    resourceData: T | null;
    isLoading: boolean;
    error: Error | null;
    pathname: string;
    navigateToResource: (resource: HalResponse | Link, options?: { state?: unknown }) => void;
    getResourceLink: (linkName?: string) => Link | null;
    refetch: () => Promise<void>;
}

export function useHalPageData<T = HalResponse>(): UseHalPageDataReturn<T> {
    const halRoute = useHalRoute();

    return {
        resourceData: halRoute.resourceData as T | null,
        isLoading: halRoute.isLoading,
        error: halRoute.error,
        pathname: halRoute.pathname,
        navigateToResource: halRoute.navigateToResource,
        getResourceLink: halRoute.getResourceLink,
        refetch: halRoute.refetch,
    };
}
