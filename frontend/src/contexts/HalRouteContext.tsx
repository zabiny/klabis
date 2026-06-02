import React, {type PropsWithChildren} from 'react';
import {type Path, useLocation, useNavigate} from 'react-router-dom';
import type {HalResourceLinks, HalResponse, Link} from '../api';
import {extractNavigationPath} from "../utils/navigationPath.ts";
import {isHalResponse} from "../components/HalNavigator2/halforms/utils.ts";
import {normalizeKlabisApiPath} from "../utils/halFormsUtils.ts";
import {useAuthorizedQuery} from "../hooks/useAuthorizedFetch.ts";
import {toHref} from "../api/hateoas.ts";
import {HalRouteContext, useHalRoute} from './halRouteContext';

interface HalRouteProviderProps {
    children: React.ReactNode;
    routeLink?: HalResourceLinks;
}

function useHalRoutePath(routeLink?: HalResourceLinks): Path {
    const location = useLocation();
    const rawHref = routeLink && toHref(routeLink);
    // Strip URI template variables (e.g. {?page,size} or {?occurredAtFrom,occurredAtTo,type})
    // before parsing, because URL constructor and manual split do not handle them
    const routeLinkHref = rawHref?.replace(/\{[^}]*\}/g, '');
    if (!routeLinkHref) {
        return location;
    } else {
        // Check if it's a full URL or just a path
        if (routeLinkHref.includes('://')) {
            const url = new URL(routeLinkHref);
            return {pathname: url.pathname, search: url.search, hash: url.hash};
        } else {
            // It's a path, parse it manually
            const [pathAndQuery, hash] = routeLinkHref.split('#');
            const [pathname, search] = pathAndQuery.split('?');
            return {
                pathname,
                search: search ? '?' + search : '',
                hash: hash ? '#' + hash : ''
            };
        }
    }
}

/**
 * Provider component that wraps the entire app
 * Automatically fetches HAL resource data when route pathname changes
 * Uses React Query for caching, deduplication, and advanced features
 */
export const HalRouteProvider: React.FC<HalRouteProviderProps> = ({children, routeLink}) => {
    const navigate = useNavigate();
    const targetUrl = useHalRoutePath(routeLink);

    // Skip HAL fetching for login route
    const shouldFetch = !targetUrl.pathname.startsWith('/login') && !targetUrl.pathname.startsWith('/auth/callback');

    // React Query hook for fetching HAL data
    // Stale Time: 5 minutes - HAL navigation data rarely changes, but re-validate on mount
    // Cache Time (gcTime): 5 minutes - keep data cached for tab switches and quick back navigation
    // Retry: 0 - don't retry; show error immediately for better UX (user can refresh if needed)
    const resourceUrl = `${targetUrl.pathname}${targetUrl.search}${targetUrl.hash && `#${targetUrl.hash}`}`;

    const {
        data,
        isLoading,
        error,
        refetch,
        status
    } = useAuthorizedQuery<HalResponse>(normalizeKlabisApiPath(resourceUrl), {
        enabled: shouldFetch,
        staleTime: 5 * 60 * 1000,
        gcTime: 5 * 60 * 1000,
        retry: 0,
    });

    // Convert React Query status to context queryState
    let queryState: 'idle' | 'pending' | 'success' | 'error';
    if (status === 'pending') {
        queryState = 'pending';
    } else if (status === 'error') {
        queryState = 'error';
    } else if (status === 'success') {
        queryState = 'success';
    } else {
        queryState = 'idle';
    }

    const navigateToResource = (resource: HalResponse | Link, options?: { state?: unknown }): void => {
        let targetLink: Link;
        if (isHalResponse(resource)) {
            const selfLink = resource?._links?.self;
            if (!selfLink) {
                throw new Error('Self link not found in resource data - cannot fetch table data')
            }
            targetLink = Array.isArray(selfLink) ? selfLink[0] : selfLink
        } else {
            targetLink = resource;
        }

        const href = Array.isArray(targetLink) ? targetLink[0]?.href : targetLink?.href
        if (!href) {
            throw new Error('Self link href is empty')
        }

        const targetPath = extractNavigationPath(toHref(href));
        navigate(targetPath, options);
    }

    const getLink = (linkName = 'self') => {
        if (!data?._links?.[linkName]) {
            return null
        }

        const link = data?._links?.[linkName]
        const resolvedLink = Array.isArray(link)
            ? link.find(l => !l.templated) ?? null
            : link

        const href = resolvedLink?.href

        if (!href) {
            return null
        }

        return {href} as Link
    }

    const contextValue = {
        resourceData: data ?? null,
        navigateToResource,
        isLoading,
        error: error instanceof Error ? error : null,
        refetch: async () => {
            await refetch();
        },
        pathname: targetUrl.pathname,
        queryState,
        getResourceLink: getLink
    };

    return (
        <HalRouteContext.Provider value={contextValue}>
            {children}
        </HalRouteContext.Provider>
    );
};

interface HalSubresourceProviderProps {
    subresourceLinkName: string
}

export const HalSubresourceProvider: React.FC<PropsWithChildren<HalSubresourceProviderProps>> = ({
                                                                                                     subresourceLinkName,
                                                                                                     children
                                                                                                 }) => {
    const {resourceData: parentResource} = useHalRoute();

    const subresourceLink = parentResource?._links?.[subresourceLinkName];
    if (subresourceLink) {
        return <HalRouteProvider routeLink={subresourceLink}>{children}</HalRouteProvider>;
    } else {
        return <div>Subresource {subresourceLinkName} wasn't found</div>;
    }
}
