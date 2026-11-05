import {toApiHref} from './apiPath';
import {klabisAuthUserManager, silentRenewRetry} from './klabisUserManager';
import {ApiError} from './apiError';

export {ApiError};
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

const PROBLEM_JSON = 'application/problem+json';
const DROPPED_PROPERTIES = new Set(['title', 'instance', 'status']);
const MAPPED_PROPERTIES = new Set(['type', 'detail', 'field', 'fieldErrors', 'errors', 'parameterErrors']);

const isRecord = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null && !Array.isArray(value);

const asString = (value: unknown): string | undefined => typeof value === 'string' ? value : undefined;

const asStringArray = (value: unknown): string[] =>
    Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : [];

const asStringRecord = (value: unknown): Record<string, string> =>
    isRecord(value)
        ? Object.fromEntries(Object.entries(value).filter((entry): entry is [string, string] => typeof entry[1] === 'string'))
        : {};

async function readProblem(response: Response): Promise<Record<string, unknown> | undefined> {
    if (!(response.headers.get('Content-Type') ?? '').includes(PROBLEM_JSON)) {
        return undefined;
    }
    try {
        const body: unknown = await response.clone().json();
        return isRecord(body) ? body : undefined;
    } catch {
        return undefined;
    }
}

async function buildFetchError(response: Response): Promise<ApiError> {
    const problem = await readProblem(response);
    if (!problem) {
        return new ApiError(response.status);
    }
    const params = Object.fromEntries(
        Object.entries(problem).filter(([key]) => !DROPPED_PROPERTIES.has(key) && !MAPPED_PROPERTIES.has(key))
    );
    return new ApiError(response.status, {
        type: asString(problem.type),
        detail: asString(problem.detail),
        field: asString(problem.field),
        fieldErrors: asStringRecord(problem.fieldErrors),
        errors: [...asStringArray(problem.errors), ...asStringArray(problem.parameterErrors)],
        params,
    });
}
