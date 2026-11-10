import {type HalRouteContextValue, useHalRoute} from '../contexts/halRouteContext';
import type {HalResponse} from '../api';

export type UseHalPageDataReturn<T = HalResponse> =
    Pick<HalRouteContextValue, 'isLoading' | 'error' | 'navigateToResource' | 'getResourceLink' | 'refetch'>
    & { resourceData: T | null };

export function useHalPageData<T = HalResponse>(): UseHalPageDataReturn<T> {
    const {resourceData, isLoading, error, navigateToResource, getResourceLink, refetch} = useHalRoute();

    return {
        resourceData: resourceData as T | null,
        isLoading,
        error,
        navigateToResource,
        getResourceLink,
        refetch,
    };
}
