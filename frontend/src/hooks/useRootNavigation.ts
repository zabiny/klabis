import type {HalResponse, Link} from '../api';
import {extractNavigationPath} from '../utils/navigationPath';
import {useAuthorizedQuery} from "./useAuthorizedFetch.ts";
import {labels} from '../localization';

/**
 * Represents a navigation menu item derived from HAL links
 */
export interface NavigationItem {
    href: string;
    label: string;
    rel: string;
    section: 'main' | 'admin';
}

const ADMIN_RELS = new Set(['training-groups', 'category-presets', 'family-groups']);

const navLabels = labels.nav as Record<string, string>;

function convertItems(response: HalResponse): NavigationItem[] {

    if (!response || !response._links) {
        return [];
    }

    const items: NavigationItem[] = [];
    const systemRels = new Set(['self', 'curies']);

    Object.entries(response._links).forEach(([rel, linkOrLinks]) => {
        if (systemRels.has(rel)) {
            return;
        }

        const links = Array.isArray(linkOrLinks) ? linkOrLinks : [linkOrLinks];

        links.forEach((link: Link | { href?: string; title?: string }) => {
            if (link && typeof link === 'object' && 'href' in link) {
                const href = link.href as string;
                const navigationPath = extractNavigationPath(href);
                const title = (link as { title?: string }).title;
                const label = navLabels[rel] ?? title ?? rel;

                items.push({
                    href: navigationPath,
                    label,
                    rel,
                    section: ADMIN_RELS.has(rel) ? 'admin' : 'main',
                });
            }
        });
    });

    return items;
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
    return useAuthorizedQuery('/api', {
        select: (data) => convertItems(data as HalResponse),
        staleTime: 5 * 60 * 1000, // 5 minutes
        retry: 1,
    });
}
