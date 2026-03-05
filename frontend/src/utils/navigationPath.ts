/**
 * Extracts the navigation path from a full URL for use with React Router.
 * Removes the /api prefix since HalRouteContext adds it back.
 *
 * Example: https://localhost:8443/api/members/123?view=full -> /members/123?view=full
 *
 * @param url Full URL or path from HAL link
 * @returns Relative path for React Router navigation
 */
export function extractNavigationPath(url: string): string {
    // Remove URI template variables first (e.g., {?status}) - common in HAL templated links
    let cleanUrl = url.replace(/\{[^}]*\}/g, '');

    // it's already path when starts with '/', remove /api prefix if present
    if (cleanUrl.startsWith('/api')) {
        return cleanUrl.substring(4);
    } else if (cleanUrl.startsWith("/")) {
        return cleanUrl;
    }

    // Parse absolute URL and extract pathname + search
    const parsedUrl = new URL(cleanUrl);
    let path = parsedUrl.pathname + parsedUrl.search;

    // Remove /api prefix if present, since HalRouteContext adds it back
    if (path.startsWith('/api')) {
        path = path.substring(4); // Remove '/api'
    }

    return path;
}
