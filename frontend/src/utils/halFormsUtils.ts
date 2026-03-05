/**
 * Utility functions for HAL Forms handling
 */

/**
 * Normalize an API path by removing /api prefix if present
 * Backend HAL links don't include /api prefix, but frontend needs to add it when making requests
 * @param path - The path or URL to normalize
 * @returns The normalized path without /api prefix
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

        // Remove /api prefix if present (backend doesn't include it in HAL links)
        if (normalized.startsWith('/api')) {
            return normalized.substring('/api'.length);
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
