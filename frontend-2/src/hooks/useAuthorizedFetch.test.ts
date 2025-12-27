import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import React from 'react';
import {useAuthorizedMutation, useAuthorizedQuery} from './useAuthorizedFetch';
import {authorizedFetch} from '../api/authorizedFetch';

jest.mock('../api/authorizedFetch');

describe('useAuthorizedQuery', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        jest.clearAllMocks();
    });

    const mockAuthorizedFetch = authorizedFetch as jest.MockedFunction<typeof authorizedFetch>;

    const createWrapper = () => {
        return ({children}: { children: ReactNode }) =>
            React.createElement(QueryClientProvider, {client: queryClient}, children);
    };

    describe('Basic Query Operations', () => {
        it('should fetch data successfully', async () => {
            const mockData = {id: 1, name: 'Test'};
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue(mockData),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

            const {result} = renderHook(() => useAuthorizedQuery('/api/items/1'), {
                wrapper: createWrapper(),
            });

            expect(result.current.isLoading).toBe(true);

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.data).toEqual(mockData);
            expect(result.current.error).toBeNull();
            expect(mockAuthorizedFetch).toHaveBeenCalledWith('/api/items/1', {}, true);
        });

        it('should handle empty response', async () => {
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue(null),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

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
            const errorText = 'Bad request';
            const mockResponse = {
                ok: false,
                status: 400,
                statusText: 'Bad Request',
                json: jest.fn().mockRejectedValue(new Error('Not JSON')),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

            const {result} = renderHook(() => useAuthorizedQuery('/api/items/1'), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.error).toBeDefined();
        });
    });

    describe('Error Handling', () => {
        it('should handle HTTP errors', async () => {
            const errorMessage = 'HTTP 404: Not Found';
            mockAuthorizedFetch.mockRejectedValue(new Error(errorMessage));

            const {result} = renderHook(() => useAuthorizedQuery('/api/items/999'), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.error).toBeDefined();
            expect(result.current.error?.message).toBe(errorMessage);
            expect(result.current.data).toBeUndefined();
        });

        it('should handle network errors', async () => {
            const networkError = new Error('Network request failed');
            mockAuthorizedFetch.mockRejectedValue(networkError);

            const {result} = renderHook(() => useAuthorizedQuery('/api/items'), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.error).toEqual(networkError);
        });
    });

    describe('Request Configuration', () => {
        it('should pass custom headers to authorizedFetch', async () => {
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue({data: 'test'}),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

            const customHeaders = {'X-Custom-Header': 'value'};
            renderHook(() => useAuthorizedQuery('/api/items', {headers: customHeaders}), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(mockAuthorizedFetch).toHaveBeenCalledWith(
                    '/api/items',
                    {headers: customHeaders},
                    true
                );
            });
        });

        it('should respect enabled flag', async () => {
            renderHook(() => useAuthorizedQuery('/api/items', {enabled: false}), {
                wrapper: createWrapper(),
            });

            // Should not call authorizedFetch when disabled
            expect(mockAuthorizedFetch).not.toHaveBeenCalled();
        });

        it('should allow custom staleTime', async () => {
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue({data: 'test'}),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

            const {result: result1} = renderHook(() => useAuthorizedQuery('/api/items', {staleTime: Infinity}), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result1.current.isLoading).toBe(false);
            });

            expect(mockAuthorizedFetch).toHaveBeenCalledTimes(1);

            // Second hook should use cache if staleTime is Infinity
            const {result: result2} = renderHook(() => useAuthorizedQuery('/api/items', {staleTime: Infinity}), {
                wrapper: createWrapper(),
            });

            // Should still only be called once due to caching
            expect(mockAuthorizedFetch).toHaveBeenCalledTimes(1);
            expect(result2.current.data).toEqual(result1.current.data);
        });
    });

    describe('Data Transformation', () => {
        it('should apply select transformation', async () => {
            const mockData = {id: 1, name: 'Test', secret: 'hidden'};
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue(mockData),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

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
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue(null),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

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
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue(mockData),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

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
            expect(mockAuthorizedFetch).toHaveBeenCalledTimes(1);
        });
    });

    describe('Loading States', () => {
        it('should transition from loading to loaded', async () => {
            const mockData = {id: 1};
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue(mockData),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

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
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        jest.clearAllMocks();
    });

    const mockAuthorizedFetch = authorizedFetch as jest.MockedFunction<typeof authorizedFetch>;

    const createWrapper = () => {
        return ({children}: { children: ReactNode }) =>
            React.createElement(QueryClientProvider, {client: queryClient}, children);
    };

    describe('Basic Mutation Operations', () => {
        it('should execute POST mutation successfully', async () => {
            const requestData = {name: 'New Item'};
            const responseData = {id: 1, name: 'New Item'};
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue(responseData),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

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
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue(responseData),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

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
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue({}),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

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
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue(null),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

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
            mockAuthorizedFetch.mockRejectedValue(new Error(errorMessage));

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {}});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            expect(result.current.error).toBeDefined();
            expect(result.current.error?.message).toBe(errorMessage);
        });

        it('should handle network errors during mutation', async () => {
            const networkError = new Error('Network failed');
            mockAuthorizedFetch.mockRejectedValue(networkError);

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {}});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            expect(result.current.error).toEqual(networkError);
        });
    });

    describe('Callbacks', () => {
        it('should call onSuccess callback after successful mutation', async () => {
            const responseData = {id: 1, name: 'New Item'};
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue(responseData),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

            const onSuccess = jest.fn();
            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST', onSuccess}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {}});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            // onSuccess is called with (data, variables, context) - context is optional
            expect(onSuccess).toHaveBeenCalled();
            const [data] = onSuccess.mock.calls[0];
            expect(data).toEqual(responseData);
        });

        it('should call onError callback after failed mutation', async () => {
            const errorMessage = 'HTTP 500: Server Error';
            const error = new Error(errorMessage);
            mockAuthorizedFetch.mockRejectedValue(error);

            const onError = jest.fn();
            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST', onError}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {}});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            // onError is called with (error, variables, context) - context is optional
            expect(onError).toHaveBeenCalled();
            const [callError] = onError.mock.calls[0];
            expect(callError).toEqual(error);
        });

        it('should call both onSettled callbacks', async () => {
            const responseData = {id: 1};
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue(responseData),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

            const onSettled = jest.fn();
            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST', onSettled}),
                {wrapper: createWrapper()}
            );

            result.current.mutate({url: '/api/items', data: {}});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            expect(onSettled).toHaveBeenCalledTimes(1);
        });
    });

    describe('Request Configuration', () => {
        it('should send request body as JSON by default', async () => {
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue({}),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

            const {result} = renderHook(
                () => useAuthorizedMutation({method: 'POST'}),
                {wrapper: createWrapper()}
            );

            const requestData = {name: 'Test'};
            result.current.mutate({url: '/api/items', data: requestData});

            await waitFor(() => {
                expect(result.current.isPending).toBe(false);
            });

            expect(mockAuthorizedFetch).toHaveBeenCalledWith(
                '/api/items',
                {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(requestData),
                },
                true
            );
        });

        it('should allow custom headers in mutation', async () => {
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue({}),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

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

            const calls = mockAuthorizedFetch.mock.calls;
            const lastCall = calls[calls.length - 1];
            expect(lastCall[1]).toEqual(
                expect.objectContaining({
                    headers: expect.objectContaining({'X-Custom': 'value'}),
                })
            );
        });
    });

    describe('Loading States', () => {
        it('should transition from pending to idle', async () => {
            const mockResponse = {
                ok: true,
                json: jest.fn().mockResolvedValue({id: 1}),
            } as any;
            mockAuthorizedFetch.mockResolvedValue(mockResponse);

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
