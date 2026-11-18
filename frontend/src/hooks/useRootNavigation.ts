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
    section: 'main' | 'admin' | 'personal';
}

const ADMIN_RELS = new Set(['admin', 'training-groups', 'legalGuardianGroups', 'membership-fees', 'syncOverview']);

export const EVENT_SETTINGS_TAB_RELS = ['disciplines', 'event-types', 'category-presets'] as const;
const EVENT_SETTINGS_REL = 'event-settings';

const PERSONAL_RELS = new Set(['profile', 'notifications']);

const navLabels = labels.nav as Record<string, string>;

function sectionOf(rel: string): NavigationItem['section'] {
    if (PERSONAL_RELS.has(rel)) return 'personal';
    return ADMIN_RELS.has(rel) ? 'admin' : 'main';
}

const SYSTEM_RELS = new Set(['self', 'curies', 'orisClubKey']);
const isEventSettingsTabRel = (rel: string) => (EVENT_SETTINGS_TAB_RELS as readonly string[]).includes(rel);

function toItems(response: HalResponse): NavigationItem[] {
    if (!response || !response._links) {
        return [];
    }

    const items: NavigationItem[] = [];

    Object.entries(response._links).forEach(([rel, linkOrLinks]) => {
        if (SYSTEM_RELS.has(rel)) {
            return;
        }

        const links = Array.isArray(linkOrLinks) ? linkOrLinks : [linkOrLinks];

        links.forEach((link: Link | { href?: string; title?: string }) => {
            if (link && typeof link === 'object' && 'href' in link) {
                const title = (link as { title?: string }).title;
                items.push({
                    href: toRoutePath(link.href as string),
                    label: navLabels[rel] ?? title ?? rel,
                    rel,
                    section: sectionOf(rel),
                });
            }
        });
    });

    return items;
}

function convertItems(response: HalResponse): NavigationItem[] {
    const result: NavigationItem[] = [];
    let entryAdded = false;

    toItems(response).forEach(item => {
        if (!isEventSettingsTabRel(item.rel)) {
            result.push(item);
        } else if (!entryAdded) {
            entryAdded = true;
            const firstTabRel = EVENT_SETTINGS_TAB_RELS.find(rel => hasLink(response, rel));
            result.push({
                href: `/event-settings/${firstTabRel}`,
                label: navLabels[EVENT_SETTINGS_REL],
                rel: EVENT_SETTINGS_REL,
                section: 'admin',
            });
        }
    });

    return result;
}

function hasLink(response: HalResponse, rel: string): boolean {
    return !!response._links?.[rel];
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

export function useEventSettingsTabs() {
    const isAuthReady = useIsAuthReady();

    return useAuthorizedQuery('/api', {
        enabled: isAuthReady,
        select: (data) => {
            const items = toItems(data as HalResponse);
            return EVENT_SETTINGS_TAB_RELS
                .map(rel => items.find(i => i.rel === rel))
                .filter((i): i is NavigationItem => !!i);
        },
        staleTime: 5 * 60 * 1000,
        retry: 1,
    });
}
