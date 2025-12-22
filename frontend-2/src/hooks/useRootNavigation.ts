import {useQuery} from '@tanstack/react-query';
import {fetchResource} from '../components/HalNavigator/hooks';
import type {HalResponse, Link} from '../api';

/**
 * Represents a navigation menu item derived from HAL links
 */
export interface NavigationItem {
    href: string;
    label: string;
    rel: string;
}

/**
 * Extracts the navigation path from a full URL for use with React Router.
 * Removes the /api prefix since HalRouteContext adds it back.
 *
 * @param url Full URL or path from HAL link
 * @returns Relative path for React Router navigation
 */
function extractNavigationPath(url: string): string {
    try {
        const parsedUrl = new URL(url);
        let path = parsedUrl.pathname;

        // Remove /api prefix if present, since HalRouteContext adds it back
        if (path.startsWith('/api')) {
            path = path.substring(4); // Remove '/api'
        }

        return path;
    } catch {
        // If URL parsing fails, assume it's already a path
        // Remove /api prefix if present
        if (url.startsWith('/api')) {
            return url.substring(4);
        }
        return url;
    }
}

/**
 * Hook to fetch and parse root navigation links from /api HAL endpoint
 *
 * Returns menu items derived from the root resource's _links, filtering out
 * system links and using title attributes when available.
 *
 * @returns {Object} with items array, loading state, and error state
 */
export function useRootNavigation() {
    return useQuery({
        queryKey: ['/api'],
        queryFn: async (): Promise<NavigationItem[]> => {
            const response = (await fetchResource('/api')) as HalResponse;

            if (!response._links) {
                return [];
            }

            const items: NavigationItem[] = [];
            const systemRels = new Set(['self', 'curies']);

            // Process each link in the _links object
            Object.entries(response._links).forEach(([rel, linkOrLinks]) => {
                // Skip system relations
                if (systemRels.has(rel)) {
                    return;
                }

                // Handle both single link and array of links
                const links = Array.isArray(linkOrLinks) ? linkOrLinks : [linkOrLinks];

                links.forEach((link: Link | { href?: string; title?: string }) => {
                    if (link && typeof link === 'object' && 'href' in link) {
                        const href = link.href as string;
                        // Extract navigation path for React Router (removes full URL, keeps just the path)
                        const navigationPath = extractNavigationPath(href);
                        // Use title if available, otherwise use the rel name as label
                        const label = (link as { title?: string }).title || rel;

                        items.push({
                            href: navigationPath,
                            label,
                            rel,
                        });
                    }
                });
            });

            return items;
        },
        staleTime: 5 * 60 * 1000, // 5 minutes
        retry: 1,
    });
}
