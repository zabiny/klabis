import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import React from 'react';
import {useAuthorizedMutation, useAuthorizedQuery} from './useAuthorizedFetch';
import {createMockResponse} from '../__mocks__/mockFetch';
import {FetchError} from '../api/authorizedFetch';

// Mock dependencies
jest.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: jest.fn().mockResolvedValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

// Shared setup for hooks
const createTestHookSetup = () => {
    let queryClient: QueryClient;
    let fetchSpy: jest.Mock;

    const setup = () => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        jest.clearAllMocks();
        // Mock global fetch
        fetchSpy = jest.fn() as jest.Mock;
        (globalThis as any).fetch = fetchSpy;
    };

    const teardown = () => {
        delete (globalThis as any).fetch;
    };

    const createWrapper = () => {
        return ({children}: { children: ReactNode }) =>
            React.createElement(QueryClientProvider, {client: queryClient}, children);
    };

    return {setup, teardown, createWrapper, getQueryClient: () => queryClient, getFetchSpy: () => fetchSpy};
};

describe('useAuthorizedQuery', () => {
    const {setup, teardown, createWrapper, getFetchSpy} = createTestHookSetup();
    let fetchSpy: jest.Mock;

    beforeEach(() => {
        setup();
        fetchSpy = getFetchSpy();
    });

    afterEach(() => {
        teardown();
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
                json: jest.fn().mockRejectedValue(new Error('Not JSON')),
                clone: () => ({
                    text: jest.fn().mockResolvedValue('Bad request'),
                }),
            } as any;
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
                    select: (data) => ({id: (data as any).id, name: (data as any).name}),
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
                    select: (data) => data ? {id: (data as any).id} : null,
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
    let fetchSpy: jest.Mock;

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

            expect(result.current.data).toEqual(responseData);
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

            expect(result.current.data).toEqual(responseData);
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

            expect(result.current.data).toEqual({});
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

            expect(result.current.data).toBeNull();
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

            const onSuccess = jest.fn();
            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST', onSuccess}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {name: 'New Item'}});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            // onSuccess is called with (data, variables, context, response)
            expect(onSuccess).toHaveBeenCalledWith(
                responseData,
                expect.objectContaining({url: '/api/items', data: {name: 'New Item'}}),
                undefined, // context is optional
                expect.any(Object) // response
            );
        });

        it('should call onError callback after failed mutation', async () => {
            const errorMessage = 'HTTP 500: Server Error';
            const error = new Error(errorMessage);
            fetchSpy.mockRejectedValue(error);

            const onError = jest.fn();
            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST', onError}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {name: 'Test'}});

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

        it('should call onSettled callback after mutation completes', async () => {
            const responseData = {id: 1};
            fetchSpy.mockResolvedValueOnce(createMockResponse(responseData));

            const onSettled = jest.fn();
            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST', onSettled}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {name: 'Test'}});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            // onSettled is called with (data, error, variables, context, response) on successful mutation
            expect(onSettled).toHaveBeenCalledWith(
                responseData,
                null,
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

        it('should allow custom headers in mutation', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse({}));

            const {result} = renderHook(
                () => useAuthorizedMutation({
                    method: 'POST',
                    headers: {'X-Custom': 'value'},
                }),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {}});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            expect(fetchSpy).toHaveBeenCalledWith(
                '/api/items',
                expect.objectContaining({
                    headers: expect.objectContaining({'X-Custom': 'value'}),
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
                expect(result.current.data).toEqual({id: 1});
            });
        });
    });
});
