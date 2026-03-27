import {klabisAuthUserManager} from './klabisUserManager';

/**
 * Fetch with automatic Bearer token injection and /api prefix for relative URLs.
 * Throws FetchError on non-ok responses unless throwOnError is false.
 */
export async function authorizedFetch(
    url: string | URL,
    options?: RequestInit,
    throwOnError: boolean = true
): Promise<Response> {
    const user = await klabisAuthUserManager.getUser();

    // Prepend /api prefix to relative URLs
    let fetchUrl: string;
    if (typeof url === 'string') {
        // If URL is absolute (starts with http:// or https://), use as-is
        // If URL already starts with /api, use as-is
        // Otherwise, prepend /api
        if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('/api')) {
            fetchUrl = url;
        } else {
            fetchUrl = `/api${url.startsWith('/') ? '' : '/'}${url}`;
        }
    } else {
        // URL object - convert to string and check if absolute
        fetchUrl = url.toString();
    }

    const headers: HeadersInit = {
        Accept: "application/prs.hal-forms+json,application/hal+json,application/json",
        ...options?.headers,
        Authorization: `Bearer ${user?.access_token}`,
    };

    const response = await fetch(fetchUrl, {
        ...options,
        headers,
    });

    if (response.status === 401) {
        await klabisAuthUserManager.removeUser();
    }

    if (!response.ok && throwOnError) {
        let errorBody: string | undefined;
        try {
            errorBody = await response.clone().text();
        } catch {
            // Ignore if we can't read the body
        }
        throw new FetchError(`HTTP ${response.status} (${response.statusText})`, response.status, response.statusText, response.headers, errorBody);
    }
    return response;
}


export class FetchError extends Error {
    public responseBody?: string;
    public responseStatus: number;
    public responseStatusText: string;
    public responseHeaders: Headers;

    constructor(message: string, responseStatus: number, responseStatusText: string, responseHeaders: Headers, responseBody?: string) {
        super(message);
        this.responseBody = responseBody;
        this.responseStatus = responseStatus;
        this.responseStatusText = responseStatusText;
        this.responseHeaders = responseHeaders;
    }

}