import {useHalRoute} from '../contexts/halRouteContext';
import type {HalEnvelope, HalResponse, Link} from '../api';

export interface UseHalPageDataReturn<T = HalResponse> {
    resourceData: T | null;
    isLoading: boolean;
    error: Error | null;
    navigateToResource: (resource: HalEnvelope | Link, options?: { state?: unknown }) => void;
    getResourceLink: (linkName?: string) => Link | null;
    refetch: () => Promise<void>;
}

export function useHalPageData<T = HalResponse>(): UseHalPageDataReturn<T> {
    const halRoute = useHalRoute();

    return {
        resourceData: halRoute.resourceData as T | null,
        isLoading: halRoute.isLoading,
        error: halRoute.error,
        navigateToResource: halRoute.navigateToResource,
        getResourceLink: halRoute.getResourceLink,
        refetch: halRoute.refetch,
    };
}
