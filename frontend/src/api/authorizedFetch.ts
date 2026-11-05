import {toApiHref} from './apiPath';
import {klabisAuthUserManager, silentRenewRetry} from './klabisUserManager';
import {ApiError} from './apiError';
import {buildFetchError} from './problemFetch';

export {ApiError, buildFetchError};
export type {ApiErrorDetails, ErrorType} from './apiError';

/**
 * Fetch with automatic Bearer token injection and /api prefix for relative URLs.
 * Throws ApiError on non-ok responses unless throwOnError is false.
 */
export async function authorizedFetch(
    url: string | URL,
    options?: RequestInit,
    throwOnError: boolean = true
): Promise<Response> {
    const response = await executeRequest(url, options);

    if (response.status === 401) {
        await silentRenewRetry(klabisAuthUserManager);
        // Retry once after successful token renewal — new token fetched fresh from user manager
        const retryResponse = await executeRequest(url, options);
        if (!retryResponse.ok && throwOnError) {
            throw await buildFetchError(retryResponse);
        }
        return retryResponse;
    }

    if (!response.ok && throwOnError) {
        throw await buildFetchError(response);
    }
    return response;
}

async function executeRequest(url: string | URL, options?: RequestInit): Promise<Response> {
    const user = await klabisAuthUserManager.getUser();
    const fetchUrl = resolveUrl(url);

    const headers: HeadersInit = {
        Accept: "application/prs.hal-forms+json,application/hal+json,application/json",
        ...options?.headers,
        Authorization: `Bearer ${user?.access_token}`,
    };

    return fetch(fetchUrl, {
        ...options,
        headers,
    });
}

// String URLs are always backend API hrefs (origin dropped, off-origin absolute strings unsupported); URL instances pass through as-is.
function resolveUrl(url: string | URL): string {
    return typeof url === 'string' ? toApiHref(url) : url.toString();
}
