import type {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {useMutation, useQuery} from '@tanstack/react-query';
import {authorizedFetch} from '../api/authorizedFetch';
import type {Path} from "react-router-dom";

/**
 * Options for useAuthorizedQuery hook
 */
export interface UseAuthorizedQueryOptions<T = unknown> {
    /**
     * Custom headers to include in the request
     */
    headers?: Record<string, string>;

    /**
     * Whether to enable this query (default: true)
     */
    enabled?: boolean;

    /**
     * Time in milliseconds until cached data is considered stale (default: 0)
     */
    staleTime?: number;

    gcTime?: number,

    retry?: number | boolean,

    /**
     * Transform function to process fetched data
     */
    select?: (data: unknown) => T;
}

const toPath = (url: string): Path => {
    if (!url) {
        return {pathname: '', hash: '', search: ''} as Path;
    }
    if (url.startsWith('/')) {
        url = `https://test.com${url}`;
    }
    try {
        const fullUrl = new URL(url);
        return {
            search: fullUrl.search, pathname: fullUrl.pathname, hash: fullUrl.hash
        } as Path;
    } catch (e) {
        console.warn('Failed URL:  ' + JSON.stringify(url))
        throw e;
    }
}

/**
 * Hook to fetch data using authorizedFetch with React Query
 *
 * Automatically handles:
 * - Authorization headers (via authorizedFetch)
 * - Loading/error states
 * - Caching and deduplication
 * - JSON parsing
 *
 * @param url - The URL to fetch
 * @param options - Configuration options
 * @returns Query result with data, loading state, and error
 *
 * @example
 * const { data, isLoading, error } = useAuthorizedQuery('/api/members', {
 *   staleTime: 5 * 60 * 1000,
 * });
 */
export function useAuthorizedQuery<T = unknown>(
    url: string,
    options?: UseAuthorizedQueryOptions<T>
): UseQueryResult<T | undefined> {
    // TODO: find way how to put there actual type from OpenApi generated schemas. Check useKlabisQuery (url: Path<API> => Result<API>?)

    const urlPath = toPath(url);

    return useQuery({
        queryKey: ['authorized', urlPath.pathname, urlPath.search || ''],
        queryFn: async () => {
            const response = await authorizedFetch(
                url,
                options?.headers ? {headers: options.headers} : {},
                true
            );
            return await response.json();
        },
        select: options?.select,
        gcTime: options?.gcTime,
        retry: options?.retry,
        enabled: options?.enabled !== false,
        staleTime: options?.staleTime ?? 0,
    });
}

/**
 * Options for useAuthorizedMutation hook
 */
export interface UseAuthorizedMutationOptions {
    /**
     * HTTP method for the mutation (POST, PUT, DELETE, etc.)
     */
    method: string;

    /**
     * Custom headers to include in all requests
     */
    headers?: Record<string, string>;

    /**
     * Called when mutation succeeds
     */
    onSuccess?: (data: unknown) => void;

    /**
     * Called when mutation fails
     */
    onError?: (error: Error) => void;

    /**
     * Called when mutation settles (succeeds or fails)
     */
    onSettled?: () => void;
}

/**
 * Variables for useAuthorizedMutation mutation function
 */
export interface UseAuthorizedMutationVariables {
    /**
     * The URL to mutate
     */
    url: string;

    /**
     * Data to send in request body (optional)
     */
    data?: unknown;
}

/**
 * Hook to mutate data using authorizedFetch with React Query
 *
 * Automatically handles:
 * - Authorization headers (via authorizedFetch)
 * - Loading/error states
 * - Request serialization (JSON by default)
 * - Success/error/settled callbacks
 *
 * @param options - Configuration options
 * @returns Mutation result with mutate function and state
 *
 * @example
 * const { mutate, isPending } = useAuthorizedMutation({
 *   method: 'POST',
 *   onSuccess: (data) => console.log('Success', data),
 *   onError: (error) => console.error('Error', error),
 * });
 *
 * mutate({ url: '/api/members', data: { name: 'John' } });
 */
export function useAuthorizedMutation(
    options: UseAuthorizedMutationOptions
): UseMutationResult<unknown, Error, UseAuthorizedMutationVariables> {
    return useMutation({
        mutationFn: async ({url, data}: UseAuthorizedMutationVariables) => {
            const headers: Record<string, string> = {
                ...options.headers,
                'Content-Type': 'application/json',
            };

            const fetchOptions: RequestInit = {
                method: options.method,
                headers,
            };

            if (data !== undefined) {
                fetchOptions.body = JSON.stringify(data);
            }

            const response = await authorizedFetch(url, fetchOptions, true);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            try {
                return await response.json();
            } catch {
                // If JSON parsing fails, return null
                return null;
            }
        },
        onSuccess: options.onSuccess,
        onError: options.onError,
        onSettled: options.onSettled,
    });
}
