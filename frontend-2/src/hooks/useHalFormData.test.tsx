import React from 'react';
import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalFormData} from './useHalFormData';
import {mockHalFormsTemplate} from '../__mocks__/halData';
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

describe('useHalFormData Hook', () => {
    let queryClient: QueryClient;
    let fetchSpy: jest.Mock;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        jest.clearAllMocks();
        // Mock global fetch
        fetchSpy = jest.fn() as jest.Mock;
        (globalThis as any).fetch = fetchSpy;
    });

    afterEach(() => {
        delete (globalThis as any).fetch;
    });

    const createWrapper = () => {
        return ({children}: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>
                {children}
            </QueryClientProvider>
        );
    };

    const currentResourceData = {id: 1, name: 'Current Resource', type: 'resource'};
    const targetResourceData = {id: 2, name: 'Target Resource', type: 'target'};

    describe('No Template Selected', () => {
        it('should return current resource data, not loading, and no errors', () => {
            const {result} = renderHook(
                () => useHalFormData(null, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.formData).toEqual(currentResourceData);
            expect(result.current.isLoadingTargetData).toBe(false);
            expect(result.current.targetFetchError).toBeNull();
            expect(fetchSpy).not.toHaveBeenCalled();
        });
    });

    describe('Target is Undefined', () => {
        it('should return current resource data when target is undefined', () => {
            const template = mockHalFormsTemplate({target: undefined});

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.formData).toEqual(currentResourceData);
        });

        it('should not fetch when target is undefined', () => {
            const template = mockHalFormsTemplate({target: undefined});

            renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(fetchSpy).not.toHaveBeenCalled();
        });
    });

    describe('Target Equals Current Path', () => {
        it('should not fetch and return current resource data when target equals current path', () => {
            const template = mockHalFormsTemplate({target: '/members/123'});

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.formData).toEqual(currentResourceData);
            expect(fetchSpy).not.toHaveBeenCalled();
        });

        it('should normalize target path and handle /api prefix differences', () => {
            const template = mockHalFormsTemplate({target: '/api/members/123'});

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.formData).toEqual(currentResourceData);
            expect(fetchSpy).not.toHaveBeenCalled();
        });
    });

    describe('Target Differs from Current Path', () => {
        it('should fetch data from target when target differs', async () => {
            const template = mockHalFormsTemplate({target: '/api/events/456'});
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            // Should be loading initially
            expect(result.current.isLoadingTargetData).toBe(true);

            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });

            expect(fetchSpy).toHaveBeenCalledWith(expect.stringContaining('events/456'), expect.any(Object));
            expect(result.current.formData).toEqual(targetResourceData);
        });

        it('should return target data after successful fetch', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetResourceData);
            });
        });

        it('should normalize target path before fetching', async () => {
            const template = mockHalFormsTemplate({target: 'events/456'});
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalledWith(expect.stringContaining('events/456'), expect.any(Object));
            });
        });

        it('should handle full URL targets', async () => {
            const template = mockHalFormsTemplate({
                target: 'https://example.com/api/events/456',
            });
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                // Full URLs have their pathname extracted, then normalized
                expect(fetchSpy).toHaveBeenCalledWith(expect.stringContaining('events/456'), expect.any(Object));
            });
        });
    });

    describe('Loading States', () => {
        it('should set isLoadingTargetData to true during fetch', () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockImplementationOnce(
                () => new Promise((resolve) => setTimeout(resolve, 100))
            );

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.isLoadingTargetData).toBe(true);
        });

        it('should set isLoadingTargetData to false after fetch completes', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });
        });

        it('should return currentResourceData while loading (fallback data)', () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockImplementationOnce(
                () => new Promise((resolve) => setTimeout(resolve, 100))
            );

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.formData).toEqual(currentResourceData);
            expect(result.current.isLoadingTargetData).toBe(true);
        });
    });

    describe('Error Handling', () => {
        it('should set error when fetch fails', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const fetchError = new Error('Network error');
            fetchSpy.mockRejectedValueOnce(fetchError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.targetFetchError).toBeTruthy();
            });
        });

        it('should set isLoadingTargetData to false after error', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockRejectedValueOnce(new Error('Failed'));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });
        });

        it('should return currentResourceData as fallback on fetch error', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockRejectedValueOnce(new Error('Failed'));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual(currentResourceData);
            });
        });

        it('should preserve error instance', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const fetchError = new Error('Specific error message');
            fetchSpy.mockRejectedValueOnce(fetchError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.targetFetchError?.message).toBe('Specific error message');
            });
        });

        it('should suppress 404 error but return fallback data', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const fetchError = new FetchError('HTTP 404 (Not Found)', 404, 'Not Found');
            fetchSpy.mockRejectedValueOnce(fetchError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                // 404 errors are suppressed (not shown to user), form shows with fallback data
                expect(result.current.formData).toEqual(currentResourceData);
                expect(result.current.targetFetchError).toBeNull();
            });
        });

        it('should suppress 405 error but return fallback data', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const fetchError = new FetchError('HTTP 405 (Method Not Allowed)', 405, 'Method Not Allowed');
            fetchSpy.mockRejectedValueOnce(fetchError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                // 405 errors are suppressed (GET not allowed), form shows with fallback data
                expect(result.current.formData).toEqual(currentResourceData);
                expect(result.current.targetFetchError).toBeNull();
            });
        });

        it('should still show error for non-404/405 HTTP errors', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const fetchError = new FetchError('HTTP 500 (Internal Server Error)', 500, 'Internal Server Error');
            fetchSpy.mockRejectedValueOnce(fetchError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.targetFetchError).toBeTruthy();
                // Still returns currentResourceData as fallback even on error
                expect(result.current.formData).toEqual(currentResourceData);
            });
        });
    });

    describe('Refetch Functionality', () => {
        it('should provide refetchTargetData function', () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.refetchTargetData).toBeInstanceOf(Function);
        });

        it('should refetch data when refetchTargetData is called', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetResourceData);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(1);

            // Refetch
            const newTargetData = {id: 3, name: 'Refreshed Data', type: 'target'};
            fetchSpy.mockResolvedValueOnce(createMockResponse(newTargetData));

            result.current.refetchTargetData();

            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalledTimes(2);
            });

            await waitFor(() => {
                expect(result.current.formData).toEqual(newTargetData);
            });
        });

        it('should handle refetch errors', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetResourceData);
            });

            // Refetch with error
            const refetchError = new Error('Refetch failed');
            fetchSpy.mockRejectedValueOnce(refetchError);

            result.current.refetchTargetData();

            await waitFor(() => {
                expect(result.current.targetFetchError).toBeTruthy();
            });
        });
    });

    describe('Template Changes', () => {
        it('should update when template changes', async () => {
            const template1 = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result, rerender} = renderHook(
                ({template}) => useHalFormData(template, currentResourceData, '/members/123'),
                {
                    wrapper: createWrapper(),
                    initialProps: {template: template1},
                }
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetResourceData);
            });

            // Change to template with same target as current path
            const template2 = mockHalFormsTemplate({target: '/members/123'});

            rerender({template: template2});

            await waitFor(() => {
                expect(result.current.formData).toEqual(currentResourceData);
            });
        });

        it('should fetch from new target when template changes', async () => {
            const template1 = mockHalFormsTemplate({target: '/events/456'});
            const targetData1 = {id: 2, name: 'Target 1'};
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetData1));

            const {result, rerender} = renderHook(
                ({template}) => useHalFormData(template, currentResourceData, '/members/123'),
                {
                    wrapper: createWrapper(),
                    initialProps: {template: template1},
                }
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetData1);
            });

            // Change to different target
            const template2 = mockHalFormsTemplate({target: '/calendar/789'});
            const targetData2 = {id: 3, name: 'Target 2'};
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetData2));

            rerender({template: template2});

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetData2);
            });
        });
    });


    describe('Return Value Properties', () => {
        it('should return all required properties', () => {
            const {result} = renderHook(
                () => useHalFormData(null, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current).toHaveProperty('formData');
            expect(result.current).toHaveProperty('isLoadingTargetData');
            expect(result.current).toHaveProperty('targetFetchError');
            expect(result.current).toHaveProperty('refetchTargetData');
        });
    });

    describe('Template Validation', () => {
        it('should handle template missing properties field', () => {
            const template = {
                ...mockHalFormsTemplate(),
                properties: undefined,
            } as any;

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            // Hook should handle missing properties gracefully
            expect(result.current).toBeDefined();
            expect(result.current.formData).toBeDefined();
        });

        it('should work correctly when template has properties field', () => {
            const template = mockHalFormsTemplate({properties: []});

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current).toBeDefined();
            expect(result.current.formData).toBeDefined();
        });
    });

    describe('Query Configuration', () => {
        it('should pass correct query options to useAuthorizedQuery', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            // Verify fetch was called (query was enabled)
            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalled();
            });
        });

        it('should not enable query when targetUrl is empty', () => {
            const template = mockHalFormsTemplate({target: ''});

            renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            // Should not fetch because target is empty
            expect(fetchSpy).not.toHaveBeenCalled();
        });

        it('should not enable query when shouldFetch is false', () => {
            const template = mockHalFormsTemplate({target: '/members/123'});

            renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            // Should not fetch because target matches current path
            expect(fetchSpy).not.toHaveBeenCalled();
        });
    });

    describe('Caching and Stale Time', () => {
        it('should fetch fresh data on mount with staleTime: 0', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockResolvedValue(createMockResponse(targetResourceData));

            // First render with a QueryClient that has more realistic settings
            const realQueryClient = new QueryClient({
                defaultOptions: {
                    queries: {retry: false, gcTime: 60000}, // Realistic cache time
                },
            });

            const wrapper = ({children}: { children: React.ReactNode }) => (
                <QueryClientProvider client={realQueryClient}>
                    {children}
                </QueryClientProvider>
            );

            const {result: result1} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper}
            );

            await waitFor(() => {
                expect(result1.current.formData).toEqual(targetResourceData);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(1);

            // The data should be cached, but with staleTime: 0, it's immediately stale
            // So a new instance should refetch immediately
            const {result: result2} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper}
            );

            await waitFor(() => {
                expect(result2.current.formData).toEqual(targetResourceData);
            });

            // Should have fetched again because staleTime is 0 (data is immediately stale)
            expect(fetchSpy).toHaveBeenCalledTimes(2);

            realQueryClient.clear();
        });
    });

    describe('Edge Cases', () => {
        it('should handle empty string currentPathname', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, ''),
                {wrapper: createWrapper()}
            );

            // Should fetch because empty pathname differs from /events/456
            expect(result.current.isLoadingTargetData).toBe(true);

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetResourceData);
            });

            expect(fetchSpy).toHaveBeenCalledWith(expect.stringContaining('events/456'), expect.any(Object));
        });

        it('should handle template with empty properties array', async () => {
            const template = mockHalFormsTemplate({
                properties: [],
                target: '/members/123' // Match current path to avoid fetch
            });

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            // Should still work, just return current resource data
            expect(result.current.formData).toEqual(currentResourceData);
            // Template matches current path, so no fetch occurs
            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });
        });

        it('should handle non-FetchError errors (standard Error instances)', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const standardError = new Error('Network timeout');
            fetchSpy.mockRejectedValueOnce(standardError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.targetFetchError).toBeTruthy();
                expect(result.current.targetFetchError).toBeInstanceOf(Error);
                expect(result.current.targetFetchError?.message).toBe('Network timeout');
            });
        });

        it('should handle template.target change during fetch', async () => {
            const template1 = mockHalFormsTemplate({target: '/events/456'});
            let resolveFirstFetch: (value: unknown) => void = () => {
            };
            const firstFetchPromise = new Promise((resolve) => {
                resolveFirstFetch = resolve;
            });
            fetchSpy.mockReturnValueOnce(firstFetchPromise);

            const {result, rerender} = renderHook(
                ({template}) => useHalFormData(template, currentResourceData, '/members/123'),
                {
                    wrapper: createWrapper(),
                    initialProps: {template: template1},
                }
            );

            // Should be loading
            expect(result.current.isLoadingTargetData).toBe(true);

            // Change template while first fetch is pending
            const template2 = mockHalFormsTemplate({target: '/calendar/789'});
            const targetData2 = {id: 3, name: 'Calendar Data'};
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetData2));

            rerender({template: template2});

            // Resolve first fetch (should be ignored due to template change)
            resolveFirstFetch(createMockResponse(targetResourceData));

            // Should fetch from new target
            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalledWith(expect.stringContaining('calendar/789'), expect.any(Object));
            });

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetData2);
            });
        });

        it('should handle empty currentResourceData object', async () => {
            const emptyData = {};
            const template = mockHalFormsTemplate({target: '/members/123'});

            const {result} = renderHook(
                () => useHalFormData(template, emptyData, '/members/123'),
                {wrapper: createWrapper()}
            );

            // Should return empty object when target matches current path
            expect(result.current.formData).toEqual(emptyData);
            // Template matches current path, so no fetch occurs
            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });
        });
    });
});
