/**
 * Utility functions for HAL Forms handling
 */

import {getApiBaseUrl} from "./getApiBaseUrl.ts";

/**
 * Normalize an API path to "relative path" with `/api` prefix (for DEV) or without `/api` (for other envs) and handling URLs
 * @param path - The path or URL to normalize
 * @returns The normalized path with /api prefix if needed (on DEV)
 */
export function normalizeKlabisApiPath(path: string): string {
    if (!path) {
        return '/';
    }

    // Try to parse as URL first
    try {
        const url = new URL(path);
        // Extract pathname and search from URL
        const fullPath = url.pathname + url.search;
        return normalizeKlabisApiPath(fullPath);
    } catch {
        // Not a valid URL, treat as a path
        let normalized = path;

        // Add leading slash if missing
        if (!normalized.startsWith('/')) {
            normalized = '/' + normalized;
        }

        // Add base URL prefix if needed
        const isDev = !!getApiBaseUrl(); // if apiBaseUrl is nonempty, it's DEV server
        console.log(`isDev: ${isDev}`)
        if (!isDev && normalized.startsWith('/api')) {
            return normalized.substring('/api'.length);
        } else if (isDev && !normalized.startsWith('/api')) {
            return `/api${normalized}`;
        }

        return normalized;
    }
}

/**
 * Determine if data should be fetched from the template target
 * @param templateTarget - The target from the HAL Forms template
 * @param currentPathname - The current resource pathname
 * @returns true if target differs from current pathname and should be fetched
 */
export function shouldFetchTargetData(
    templateTarget: string | undefined,
    currentPathname: string
): boolean {
    // No target specified - use current resource data
    if (!templateTarget || templateTarget === '') {
        return false;
    }

    const normalizedTarget = normalizeKlabisApiPath(templateTarget);
    const normalizedCurrent = normalizeKlabisApiPath(currentPathname);

    // If normalized paths are the same, no need to fetch
    return normalizedTarget !== normalizedCurrent;
}
