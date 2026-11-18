import type {HalResponse, Link} from '../api';
import {toRoutePath} from '../api/apiPath';
import {useAuthorizedQuery} from "./useAuthorizedFetch.ts";
import {labels} from '../localization';
import {useIsAuthReady} from './useIsAuthReady';

/**
 * Represents a navigation menu item derived from HAL links
 */
export interface NavigationItem {
    href: string;
    label: string;
    rel: string;
    section: 'main' | 'admin' | 'personal' | 'event-settings-tab';
}

const ADMIN_RELS = new Set(['admin', 'training-groups', 'legalGuardianGroups', 'membership-fees', 'syncOverview']);

export const EVENT_SETTINGS_TAB_RELS = ['disciplines', 'event-types', 'category-presets'] as const;
const EVENT_SETTINGS_REL = 'event-settings';

const PERSONAL_RELS = new Set(['profile', 'notifications']);

const navLabels = labels.nav as Record<string, string>;

function sectionOf(rel: string): NavigationItem['section'] {
    if ((EVENT_SETTINGS_TAB_RELS as readonly string[]).includes(rel)) return 'event-settings-tab';
    if (PERSONAL_RELS.has(rel)) return 'personal';
    return ADMIN_RELS.has(rel) ? 'admin' : 'main';
}

function withEventSettingsEntry(items: NavigationItem[]): NavigationItem[] {
    const firstIndex = items.findIndex(i => i.section === 'event-settings-tab');
    if (firstIndex < 0) return items;

    const firstTab = EVENT_SETTINGS_TAB_RELS
        .map(rel => items.find(i => i.rel === rel))
        .find((i): i is NavigationItem => !!i)!;

    const entry: NavigationItem = {
        href: `/event-settings/${firstTab.rel}`,
        label: navLabels[EVENT_SETTINGS_REL],
        rel: EVENT_SETTINGS_REL,
        section: 'admin',
    };
    return [...items.slice(0, firstIndex), entry, ...items.slice(firstIndex)];
}

function convertItems(response: HalResponse): NavigationItem[] {

    if (!response || !response._links) {
        return [];
    }

    const items: NavigationItem[] = [];
    const skippedRels = new Set(['self', 'curies', 'orisClubKey']);

    Object.entries(response._links).forEach(([rel, linkOrLinks]) => {
        if (skippedRels.has(rel)) {
            return;
        }

        const links = Array.isArray(linkOrLinks) ? linkOrLinks : [linkOrLinks];

        links.forEach((link: Link | { href?: string; title?: string }) => {
            if (link && typeof link === 'object' && 'href' in link) {
                const href = link.href as string;
                const navigationPath = toRoutePath(href);
                const title = (link as { title?: string }).title;
                const label = navLabels[rel] ?? title ?? rel;

                items.push({
                    href: navigationPath,
                    label,
                    rel,
                    section: sectionOf(rel),
                });
            }
        });
    });

    return withEventSettingsEntry(items);
}

export function useRootResource<T = HalResponse>(select?: (root: HalResponse) => T) {
    const isAuthReady = useIsAuthReady();

    return useAuthorizedQuery<T>('/api', {
        enabled: isAuthReady,
        select: select as ((data: unknown) => T) | undefined,
        staleTime: 5 * 60 * 1000,
        retry: 1,
    });
}

/**
 * Hook to fetch and parse root navigation links from /api HAL endpoint
 *
 * Returns menu items derived from the root resource's _links, filtering out
 * system links and using title attributes when available.
 */
export function useRootNavigation() {
    return useRootResource(convertItems);
}
