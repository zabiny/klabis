import {klabisAuthUserManager, silentRenewRetry} from './klabisUserManager';

/**
 * Fetch with automatic Bearer token injection and /api prefix for relative URLs.
 * Throws FetchError on non-ok responses unless throwOnError is false.
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

function resolveUrl(url: string | URL): string {
    if (typeof url !== 'string') {
        return url.toString();
    }
    if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('/api')) {
        return url;
    }
    return `/api${url.startsWith('/') ? '' : '/'}${url}`;
}

async function buildFetchError(response: Response): Promise<FetchError> {
    let errorBody: string | undefined;
    try {
        errorBody = await response.clone().text();
    } catch {
        // Ignore if we can't read the body
    }
    return new FetchError(
        `HTTP ${response.status} (${response.statusText})`,
        response.status,
        response.statusText,
        response.headers,
        errorBody
    );
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
