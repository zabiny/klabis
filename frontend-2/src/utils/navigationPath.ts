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
	// it's already path when starts with '/', remove /api prefix if present
	if (url.startsWith('/api')) {
		return url.substring(4);
	} else if (url.startsWith("/")) {
		return url;
	}

	const parsedUrl = new URL(url);
	// remove hostname and keep just path and URL params
	let path = url.substring(url.indexOf(parsedUrl.pathname));

	// Remove /api prefix if present, since HalRouteContext adds it back
	if (path.startsWith('/api')) {
		path = path.substring(4); // Remove '/api'
	}

	return path;
}
