import {klabisAuthUserManager} from './klabisUserManager';

/**
 * Performs an authorized fetch request with automatic bearer token injection.
 *
 * Retrieves the current user's access token and adds it as a Bearer token
 * in the Authorization header. Other headers are merged with provided options.
 *
 * Note: API prefix (/api in dev, empty in prod) is handled by the HAL context
 * and openapi-fetch setup, so you typically pass relative paths.
 *
 * @param url - The URL to fetch (can be relative or absolute)
 * @param options - Fetch options (optional). Headers will be merged with auth headers.
 * @param throwOnError - If true, throws on non-ok responses. Defaults to true.
 * @returns Promise containing the fetch Response
 * @throws Error if the request fails (non-2xx status code) and throwOnError is true
 *
 * @example
 * // Throws on error (default)
 * const data = await authorizedFetch('/members');
 * const json = await data.json();
 *
 * @example
 * // Returns response regardless of status
 * const res = await authorizedFetch('/submit', {}, false);
 * if (!res.ok) {
 *   // Handle error response
 * }
 */
export async function authorizedFetch(
	url: string | URL,
	options?: RequestInit,
	throwOnError: boolean = true
): Promise<Response> {
	const user = await klabisAuthUserManager.getUser();

	const headers: HeadersInit = {
		...options?.headers,
		Authorization: `Bearer ${user?.access_token}`,
	};

	const response = await fetch(url, {
		...options,
		headers,
	});

	if (!response.ok && throwOnError) {
		let errorBody: string | undefined;
		try {
			errorBody = await response.clone().text();
		} catch {
			// Ignore if we can't read the body
		}
		throw new Error(`HTTP ${response.status}: ${errorBody || response.statusText}`);
	}

	return response;
}
