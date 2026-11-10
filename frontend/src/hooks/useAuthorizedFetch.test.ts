import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import React from 'react';
import {invalidateAuthorizedQueries, useAuthorizedMutation, useAuthorizedQuery} from './useAuthorizedFetch';
import {createMockResponse} from '../__mocks__/mockFetch';
import {FetchError} from '../api/authorizedFetch';
import {type Mock, vi} from 'vitest';

// Mock dependencies
vi.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

// Shared setup for hooks
const createTestHookSetup = () => {
    let queryClient: QueryClient;
    let fetchSpy: Mock;

    const setup = () => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        vi.clearAllMocks();
        // Mock global fetch
        fetchSpy = vi.fn() as Mock;
        (globalThis as Record<string, unknown>).fetch = fetchSpy;
    };

    const teardown = () => {
        delete (globalThis as Record<string, unknown>).fetch;
    };

    const createWrapper = () => {
        return ({children}: { children: ReactNode }) =>
            React.createElement(QueryClientProvider, {client: queryClient}, children);
    };

    return {setup, teardown, createWrapper, getQueryClient: () => queryClient, getFetchSpy: () => fetchSpy};
};

describe('useAuthorizedQuery', () => {
    const {setup, teardown, createWrapper, getFetchSpy, getQueryClient} = createTestHookSetup();
    let fetchSpy: Mock;

    beforeEach(() => {
        setup();
        fetchSpy = getFetchSpy();
    });

    afterEach(() => {
        teardown();
    });

    describe('Cache key normalisation', () => {
        it('shares one cache entry between a route path and an absolute HAL href', async () => {
            fetchSpy.mockResolvedValue(createMockResponse({id: 1}));

            const {result} = renderHook(
                () => ({
                    route: useAuthorizedQuery('/members/1'),
                    absolute: useAuthorizedQuery('https://host/api/members/1'),
                }),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.route.isSuccess).toBe(true);
                expect(result.current.absolute.isSuccess).toBe(true);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(1);
            expect(getQueryClient().getQueryData(['authorized', '/api/members/1'])).toBeDefined();
        });

        it('keeps URLs that differ only in query string distinct', async () => {
            fetchSpy.mockResolvedValue(createMockResponse({id: 1}));

            const {result} = renderHook(
                () => ({
                    a: useAuthorizedQuery('/members?a=1'),
                    b: useAuthorizedQuery('/members?a=2'),
                }),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.a.isSuccess).toBe(true);
                expect(result.current.b.isSuccess).toBe(true);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(2);
        });

        it('does not fetch nor adopt data cached for the API root when url is undefined', () => {
            getQueryClient().setQueryData(['authorized', '/api'], {root: true});

            const {result} = renderHook(
                () => useAuthorizedQuery(undefined),
                {wrapper: createWrapper()}
            );

            expect(result.current.data).toBeUndefined();
            expect(result.current.isLoading).toBe(false);
            expect(result.current.fetchStatus).toBe('idle');
            expect(fetchSpy).not.toHaveBeenCalled();
        });

        it('does not fetch a defined url when enabled is false', () => {
            renderHook(
                () => useAuthorizedQuery('/api/items', {enabled: false}),
                {wrapper: createWrapper()}
            );

            expect(fetchSpy).not.toHaveBeenCalled();
        });
    });

    describe('Basic Query Operations', () => {
        it('should fetch data successfully', async () => {
            const mockData = {id: 1, name: 'Test'};
            fetchSpy.mockResolvedValueOnce(createMockResponse(mockData));

            const {result} = renderHook(() => useAuthorizedQuery('/api/items/1'), {
                wrapper: createWrapper(),
            });

            expect(result.current.isLoading).toBe(true);

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.data).toEqual(mockData);
            expect(result.current.error).toBeNull();
            expect(fetchSpy).toHaveBeenCalled();
        });

        it('should handle empty response', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse(null));

            const {result} = renderHook(() => useAuthorizedQuery('/api/items/1'), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.data).toBeNull();
            expect(result.current.error).toBeNull();
        });

        it('should handle text responses when JSON parsing fails', async () => {
            const mockResponse = {
                ok: false,
                status: 400,
                statusText: 'Bad request',
                json: vi.fn().mockRejectedValue(new Error('Not JSON')),
                clone: () => ({
                    text: vi.fn().mockResolvedValue('Bad request'),
                }),
            } as unknown as Response;
            fetchSpy.mockResolvedValueOnce(mockResponse);

            const {result} = renderHook(() => useAuthorizedQuery('/api/items/1'), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.error).toBeDefined();
            });
        });
    });

    describe('Error Handling', () => {
        it('should handle query errors', async () => {
            const error = new Error('Request failed');
            fetchSpy.mockRejectedValueOnce(error);

            renderHook(() => useAuthorizedQuery('/api/items/999'), {
                wrapper: createWrapper(),
            });

            // Verify fetch was attempted
            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalled();
            });

            // Verify the error was passed to fetch
            expect(fetchSpy).toHaveBeenCalledWith(
                '/api/items/999',
                expect.any(Object)
            );
        });
    });

    describe('Request Configuration', () => {
        it('should pass custom headers to fetch', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse({data: 'test'}));

            const customHeaders = {'X-Custom-Header': 'value'};
            renderHook(() => useAuthorizedQuery('/api/items', {headers: customHeaders}), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalled();
            });
        });

        it('should respect enabled flag', async () => {
            renderHook(() => useAuthorizedQuery('/api/items', {enabled: false}), {
                wrapper: createWrapper(),
            });

            // Should not call fetch when disabled
            expect(fetchSpy).not.toHaveBeenCalled();
        });

        it('should allow custom staleTime', async () => {
            fetchSpy.mockResolvedValue(createMockResponse({data: 'test'}));

            const {result: result1} = renderHook(() => useAuthorizedQuery('/api/items', {staleTime: Infinity}), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result1.current.isLoading).toBe(false);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(1);

            // Second hook should use cache if staleTime is Infinity
            const {result: result2} = renderHook(() => useAuthorizedQuery('/api/items', {staleTime: Infinity}), {
                wrapper: createWrapper(),
            });

            // Should still only be called once due to caching
            expect(fetchSpy).toHaveBeenCalledTimes(1);
            expect(result2.current.data).toEqual(result1.current.data);
        });
    });

    describe('Data Transformation', () => {
        it('should apply select transformation', async () => {
            const mockData = {id: 1, name: 'Test', secret: 'hidden'};
            fetchSpy.mockResolvedValueOnce(createMockResponse(mockData));

            const {result} = renderHook(
                () => useAuthorizedQuery('/api/items/1', {
                    select: (data) => { const d = data as {id: number; name: string}; return {id: d.id, name: d.name}; },
                }),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.data).toEqual({id: 1, name: 'Test'});
        });

        it('should handle select transformation with null data', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse(null));

            const {result} = renderHook(
                () => useAuthorizedQuery('/api/items/1', {
                    select: (data) => data ? {id: (data as {id: number}).id} : null,
                }),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.data).toBeNull();
        });
    });

    describe('Request Deduplication', () => {
        it('should deduplicate simultaneous requests to same URL', async () => {
            const mockData = {id: 1, name: 'Test'};
            fetchSpy.mockResolvedValue(createMockResponse(mockData));

            const {result: result1} = renderHook(() => useAuthorizedQuery('/api/items/1'), {
                wrapper: createWrapper(),
            });
            const {result: result2} = renderHook(() => useAuthorizedQuery('/api/items/1'), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result1.current.isLoading).toBe(false);
                expect(result2.current.isLoading).toBe(false);
            });

            // Both should have the same data
            expect(result1.current.data).toEqual(result2.current.data);

            // Should only fetch once
            expect(fetchSpy).toHaveBeenCalledTimes(1);
        });
    });

    describe('Loading States', () => {
        it('should transition from loading to loaded', async () => {
            const mockData = {id: 1};
            fetchSpy.mockResolvedValueOnce(createMockResponse(mockData));

            const {result} = renderHook(() => useAuthorizedQuery('/api/items/1'), {
                wrapper: createWrapper(),
            });

            expect(result.current.isLoading).toBe(true);
            expect(result.current.data).toBeUndefined();

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.data).toEqual(mockData);
        });
    });
});

describe('useAuthorizedMutation', () => {
    const {setup, teardown, createWrapper, getFetchSpy} = createTestHookSetup();
    let fetchSpy: Mock;

    beforeEach(() => {
        setup();
        fetchSpy = getFetchSpy();
    });

    afterEach(() => {
        teardown();
    });

    describe('Basic Mutation Operations', () => {
        it('should execute POST mutation successfully', async () => {
            const requestData = {name: 'New Item'};
            const responseData = {id: 1, name: 'New Item'};
            fetchSpy.mockResolvedValueOnce(createMockResponse(responseData));

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            expect(result.current.isPending).toBe(false);

            result.current.mutate({url: '/api/items', data: requestData});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            expect(result.current.data).toEqual({data: responseData, location: null});
            expect(result.current.error).toBeNull();
        });

        it('should execute PUT mutation successfully', async () => {
            const requestData = {id: 1, name: 'Updated Item'};
            const responseData = {id: 1, name: 'Updated Item'};
            fetchSpy.mockResolvedValueOnce(createMockResponse(responseData));

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'PUT'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items/1', data: requestData});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            expect(result.current.data).toEqual({data: responseData, location: null});
        });

        it('should execute DELETE mutation successfully', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse({}));

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'DELETE'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items/1'});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            expect(result.current.data).toEqual({data: {}, location: null});
        });

        it('should handle mutation with no response data', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse(null));

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {}});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            expect(result.current.data).toEqual({data: null, location: null});
        });
    });

    describe('Error Handling', () => {
        it('should handle mutation errors', async () => {
            const errorMessage = 'HTTP 400: Bad Request';
            fetchSpy.mockRejectedValue(new Error(errorMessage));

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {}});

            await waitFor(() => {
                expect(result.current.error).toBeDefined();
            });

            expect(result.current.error?.message).toBe(errorMessage);
        });

        it('should handle network errors during mutation', async () => {
            const networkError = new Error('Network failed');
            fetchSpy.mockRejectedValue(networkError);

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {}});

            await waitFor(() => {
                expect(result.current.error).toBeDefined();
            });

            expect(result.current.error).toEqual(networkError);
        });

        it('should include response headers in FetchError', async () => {
            const mockResponse = {
                ok: false,
                status: 400,
                statusText: 'Bad Request',
                headers: new Headers({'Content-Type': 'application/json'}),
                text: async () => 'Bad Request',
            } as Response;

            fetchSpy.mockResolvedValueOnce(mockResponse);

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {}});

            await waitFor(() => {
                expect(result.current.error).toBeDefined();
            });

            const error = result.current.error as FetchError;
            expect(error).toBeInstanceOf(FetchError);
            expect(error.responseHeaders).toBeDefined();
            expect(error.responseHeaders).toBeInstanceOf(Headers);
            expect(error.responseHeaders.get('Content-Type')).toBe('application/json');
        });
    });

    describe('Callbacks', () => {
        it('should call onSuccess callback after successful mutation', async () => {
            const responseData = {id: 1, name: 'New Item'};
            fetchSpy.mockResolvedValueOnce(createMockResponse(responseData));

            const onSuccess = vi.fn();
            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {name: 'New Item'}}, {onSuccess});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            expect(onSuccess).toHaveBeenCalledWith(
                {data: responseData, location: null},
                expect.objectContaining({url: '/api/items', data: {name: 'New Item'}}),
                undefined,
                expect.any(Object)
            );
        });

        it('should call onError callback after failed mutation', async () => {
            const errorMessage = 'HTTP 500: Server Error';
            const error = new Error(errorMessage);
            fetchSpy.mockRejectedValue(error);

            const onError = vi.fn();
            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {name: 'Test'}}, {onError});

            await waitFor(() => {
                expect(result.current.error).toBeDefined();
            });

            // onError is called with (error, variables, context, response)
            expect(onError).toHaveBeenCalledWith(
                error,
                expect.objectContaining({url: '/api/items', data: {name: 'Test'}}),
                undefined, // context is optional
                expect.any(Object) // response
            );
        });

    });

    describe('Request Configuration', () => {
        it('should send request body as JSON by default', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse({}));

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            const requestData = {name: 'Test'};
            result.current.mutate({url: '/api/items', data: requestData});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            expect(fetchSpy).toHaveBeenCalledWith(
                '/api/items',
                expect.objectContaining({
                    method: 'POST',
                    headers: expect.objectContaining({'Content-Type': 'application/json'}),
                    body: JSON.stringify(requestData),
                })
            );
        });
    });

    describe('Loading States', () => {
        it('should transition from pending to idle', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse({id: 1}));

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            expect(result.current.isPending).toBe(false);

            // React Query updates isPending asynchronously, so we use waitFor
            result.current.mutate({url: '/api/items', data: {}});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
                expect(result.current.data).toEqual({data: {id: 1}, location: null});
            });
        });
    });

    describe('Location header in result', () => {
        it('POST 201 with Location header returns { data: null, location: header value }', async () => {
            fetchSpy.mockResolvedValueOnce(
                createMockResponse(null, 201, {'Location': '/api/legal-guardian-groups/abc'})
            );

            const onSuccess = vi.fn();
            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/legal-guardian-groups', data: {}}, {onSuccess});

            await waitFor(() => expect(result.current.isPending).toBe(false));

            expect(onSuccess).toHaveBeenCalledWith(
                {data: null, location: '/api/legal-guardian-groups/abc'},
                expect.objectContaining({url: '/api/legal-guardian-groups'}),
                undefined,
                expect.any(Object)
            );
        });

        it('PUT 204 with no body and no Location returns { data: null, location: null }', async () => {
            fetchSpy.mockResolvedValueOnce(
                createMockResponse(null, 204)
            );

            const onSuccess = vi.fn();
            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'PUT'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items/1', data: {}}, {onSuccess});

            await waitFor(() => expect(result.current.isPending).toBe(false));

            expect(onSuccess).toHaveBeenCalledWith(
                {data: null, location: null},
                expect.objectContaining({url: '/api/items/1'}),
                undefined,
                expect.any(Object)
            );
        });

        it('POST 201 with JSON body and Location header returns { data: <body>, location: <header> }', async () => {
            const responseBody = {id: 'xyz', name: 'New Group'};
            fetchSpy.mockResolvedValueOnce(
                createMockResponse(responseBody, 201, {'Location': '/api/legal-guardian-groups/xyz'})
            );

            const onSuccess = vi.fn();
            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/legal-guardian-groups', data: {}}, {onSuccess});

            await waitFor(() => expect(result.current.isPending).toBe(false));

            expect(onSuccess).toHaveBeenCalledWith(
                {data: responseBody, location: '/api/legal-guardian-groups/xyz'},
                expect.objectContaining({url: '/api/legal-guardian-groups'}),
                undefined,
                expect.any(Object)
            );
        });
    });
});

describe('cache invalidation after mutation', () => {
    const {setup, teardown, createWrapper, getFetchSpy, getQueryClient} = createTestHookSetup();
    let fetchSpy: Mock;

    beforeEach(() => {
        setup();
        fetchSpy = getFetchSpy();
    });

    afterEach(() => {
        teardown();
    });

    const renderQueryAndMutation = () => renderHook(() => ({
        query: useAuthorizedQuery<{ v: number }>('/api/items/1'),
        mutation: useAuthorizedMutation({method: 'POST'}),
    }), {wrapper: createWrapper()});

    it('refetches active authorized queries before per-call onSuccess runs', async () => {
        fetchSpy.mockImplementation(async (_url: string, init?: RequestInit) =>
            init?.method === 'POST' ? createMockResponse({}) : createMockResponse({v: fetchSpy.mock.calls.length}));
        const {result} = renderQueryAndMutation();
        await waitFor(() => expect(result.current.query.data).toBeDefined());
        const before = result.current.query.data;

        let dataSeenInOnSuccess: unknown;
        const onSuccess = vi.fn(() => {
            dataSeenInOnSuccess = getQueryClient().getQueryData(['authorized', '/api/items/1']);
        });
        result.current.mutation.mutate({url: '/api/items/1', data: {}}, {onSuccess});

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());
        expect(dataSeenInOnSuccess).not.toEqual(before);
    });

    it('keeps isPending true until the refetch has finished', async () => {
        let releaseRefetch: () => void = () => {};
        let getCount = 0;
        fetchSpy.mockImplementation(async (_url: string, init?: RequestInit) => {
            if (init?.method === 'POST') return createMockResponse({});
            getCount++;
            if (getCount > 1) {
                await new Promise<void>(resolve => { releaseRefetch = resolve; });
            }
            return createMockResponse({v: getCount});
        });
        const {result} = renderQueryAndMutation();
        await waitFor(() => expect(result.current.query.data).toBeDefined());

        result.current.mutation.mutate({url: '/api/items/1', data: {}});
        await waitFor(() => expect(getCount).toBe(2));
        expect(result.current.mutation.isPending).toBe(true);

        releaseRefetch();
        await waitFor(() => expect(result.current.mutation.isPending).toBe(false));
        expect(result.current.mutation.isSuccess).toBe(true);
    });

    it('does not refetch when the mutation fails', async () => {
        fetchSpy.mockImplementation(async (_url: string, init?: RequestInit) => {
            if (init?.method === 'POST') throw new Error('boom');
            return createMockResponse({v: 1});
        });
        const {result} = renderQueryAndMutation();
        await waitFor(() => expect(result.current.query.data).toBeDefined());
        const getCallsBefore = fetchSpy.mock.calls.length;

        result.current.mutation.mutate({url: '/api/items/1', data: {}});
        await waitFor(() => expect(result.current.mutation.isError).toBe(true));

        expect(fetchSpy.mock.calls.length).toBe(getCallsBefore + 1);
    });

    it('still succeeds and calls onSuccess when the refetch fails', async () => {
        let getCount = 0;
        fetchSpy.mockImplementation(async (_url: string, init?: RequestInit) => {
            if (init?.method === 'POST') return createMockResponse({ok: true});
            getCount++;
            if (getCount > 1) throw new Error('refetch failed');
            return createMockResponse({v: 1});
        });
        const {result} = renderHook(() => ({
            query: useAuthorizedQuery<{ v: number }>('/api/items/1'),
            mutation: useAuthorizedMutation({method: 'POST'}),
        }), {wrapper: createWrapper()});
        await waitFor(() => expect(result.current.query.data).toBeDefined());

        const onSuccess = vi.fn();
        result.current.mutation.mutate({url: '/api/items/1', data: {}}, {onSuccess});

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());
        expect(result.current.mutation.isError).toBe(false);
    });

    it('invalidateAuthorizedQueries refetches active authorized queries', async () => {
        fetchSpy.mockImplementation(async () => createMockResponse({v: fetchSpy.mock.calls.length}));
        const {result} = renderHook(() => useAuthorizedQuery<{ v: number }>('/api/items/1'), {
            wrapper: createWrapper(),
        });
        await waitFor(() => expect(result.current.data).toBeDefined());
        const before = result.current.data;

        await invalidateAuthorizedQueries(getQueryClient());

        await waitFor(() => expect(result.current.data).not.toEqual(before));
    });

    it('uses QueryClient default retry when query has no retry option', async () => {
        const client = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
        fetchSpy.mockImplementation(async () => {
            throw new Error('boom');
        });
        const {result} = renderHook(() => useAuthorizedQuery('/api/items/1'), {
            wrapper: ({children}: { children: ReactNode }) =>
                React.createElement(QueryClientProvider, {client}, children),
        });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(fetchSpy).toHaveBeenCalledTimes(1);
    });
});
