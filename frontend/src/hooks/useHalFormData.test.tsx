import React from 'react';
import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalFormData} from './useHalFormData';
import {mockHalFormsTemplate} from '../__mocks__/halData';
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

/**
 * Creates an OPTIONS response with the given Allow header.
 * Used by useHalFormGetAvailability probe that precedes the GET.
 */
function createOptionsResponse(allowMethods: string[]): Response {
    return createMockResponse(null, 200, {'Allow': allowMethods.join(', ')});
}

describe('useHalFormData Hook', () => {
    let queryClient: QueryClient;
    let fetchSpy: Mock;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        vi.clearAllMocks();
        // Mock global fetch — both OPTIONS (probe) and GET calls go through this spy.
        // Distinguish by checking options.method in assertions.
        fetchSpy = vi.fn() as Mock;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (globalThis as any).fetch = fetchSpy;
    });

    afterEach(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
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
        it('should fire OPTIONS probe first and then GET when target differs', async () => {
            const template = mockHalFormsTemplate({target: '/api/events/456'});
            // First call: OPTIONS probe, second call: GET
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET', 'POST']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.isLoadingTargetData).toBe(true);

            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(2);
            const [optionsCall, getCall] = fetchSpy.mock.calls;
            expect(optionsCall[0]).toContain('events/456');
            expect(optionsCall[1]).toMatchObject({method: 'OPTIONS'});
            expect(getCall[0]).toContain('events/456');
            expect(getCall[1]).not.toMatchObject({method: 'OPTIONS'});
            expect(result.current.formData).toEqual(targetResourceData);
        });

        it('should return target data after successful OPTIONS probe + GET', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET', 'PUT']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

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
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

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
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

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

    describe('OPTIONS-gated GET behaviour', () => {
        it('should NOT fire GET when OPTIONS Allow header does not include GET', async () => {
            const template = mockHalFormsTemplate({target: '/api/events/456'});
            fetchSpy.mockResolvedValueOnce(createOptionsResponse(['POST', 'PUT']));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(1);
            expect(fetchSpy.mock.calls[0][1]).toMatchObject({method: 'OPTIONS'});
            expect(result.current.formData).toEqual(currentResourceData);
            expect(result.current.targetFetchError).toBeNull();
        });

        it('should return currentResourceData when OPTIONS says GET is not allowed', async () => {
            const template = mockHalFormsTemplate({target: '/api/events/456'});
            fetchSpy.mockResolvedValueOnce(createOptionsResponse(['POST']));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });

            expect(result.current.formData).toEqual(currentResourceData);
        });

        it('should fire GET when OPTIONS Allow header includes GET', async () => {
            const template = mockHalFormsTemplate({target: '/api/events/456'});
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET', 'POST']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetResourceData);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(2);
        });

        it('should surface OPTIONS error through targetFetchError and skip GET', async () => {
            const template = mockHalFormsTemplate({target: '/api/events/456'});
            const optionsError = new FetchError('HTTP 500 (Internal Server Error)', 500, 'Internal Server Error', new Headers());
            fetchSpy.mockRejectedValueOnce(optionsError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.targetFetchError).toBeTruthy();
            });

            // Only OPTIONS was called, GET was skipped
            expect(fetchSpy).toHaveBeenCalledTimes(1);
            expect(fetchSpy.mock.calls[0][1]).toMatchObject({method: 'OPTIONS'});
        });

        it('should NOT fire OPTIONS probe when shouldFetch is false (target = current path)', () => {
            const template = mockHalFormsTemplate({target: '/members/123'});

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(fetchSpy).not.toHaveBeenCalled();
            expect(result.current.formData).toEqual(currentResourceData);
        });

        it('isLoadingTargetData should be true while OPTIONS is in flight', () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            // OPTIONS never resolves during this test
            fetchSpy.mockImplementationOnce(
                () => new Promise((resolve) => setTimeout(resolve, 100))
            );

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.isLoadingTargetData).toBe(true);
        });
    });

    describe('Loading States', () => {
        it('should set isLoadingTargetData to true during fetch (OPTIONS phase)', () => {
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

        it('should set isLoadingTargetData to false after OPTIONS + GET complete', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

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
        it('should set error when OPTIONS probe fails', async () => {
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

        it('should set isLoadingTargetData to false after OPTIONS probe error', async () => {
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

        it('should return currentResourceData as fallback when OPTIONS probe errors', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy.mockRejectedValueOnce(new Error('Failed'));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.targetFetchError).toBeTruthy();
            });

            // formData falls back to currentResourceData since no GET was issued
            expect(result.current.formData).toEqual(currentResourceData);
        });

        it('should preserve OPTIONS error instance', async () => {
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

        it('should surface GET error (after successful OPTIONS) through targetFetchError', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockRejectedValueOnce(new FetchError('HTTP 500 (Internal Server Error)', 500, 'Internal Server Error', new Headers()));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.targetFetchError).toBeTruthy();
                expect(result.current.formData).toEqual(currentResourceData);
            });
        });

        it('should not fire GET when OPTIONS returns 404 (endpoint does not support OPTIONS)', async () => {
            // Replaces old "suppress 404 error" test — now handled by OPTIONS probe, not GET 404-swallowing
            const template = mockHalFormsTemplate({target: '/events/456'});
            const optionsError = new FetchError('HTTP 404 (Not Found)', 404, 'Not Found', new Headers());
            fetchSpy.mockRejectedValueOnce(optionsError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });

            // Only OPTIONS was called — GET was never issued
            expect(fetchSpy).toHaveBeenCalledTimes(1);
            // OPTIONS error surfaces (no silent suppression at GET level anymore)
            expect(result.current.targetFetchError).toBeTruthy();
        });

        it('should show error for HTTP 500 from OPTIONS probe', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const fetchError = new FetchError('HTTP 500 (Internal Server Error)', 500, 'Internal Server Error', new Headers());
            fetchSpy.mockRejectedValueOnce(fetchError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.targetFetchError).toBeTruthy();
                expect(result.current.formData).toEqual(currentResourceData);
            });
        });
    });

    describe('Refetch Functionality', () => {
        it('should provide refetchTargetData function', () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.refetchTargetData).toBeInstanceOf(Function);
        });

        it('should refetch GET data when refetchTargetData is called (OPTIONS cached)', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            // OPTIONS (cached after first call) + GET
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetResourceData);
            });

            // OPTIONS (1) + GET (1) = 2 calls initially
            expect(fetchSpy).toHaveBeenCalledTimes(2);

            // Refetch — OPTIONS is cached (staleTime 10min), only GET is repeated
            const newTargetData = {id: 3, name: 'Refreshed Data', type: 'target'};
            fetchSpy.mockResolvedValueOnce(createMockResponse(newTargetData));

            result.current.refetchTargetData();

            await waitFor(() => {
                expect(result.current.formData).toEqual(newTargetData);
            });

            // Total: OPTIONS(1) + GET(1) + GET-refetch(1) = 3
            expect(fetchSpy).toHaveBeenCalledTimes(3);
        });

        it('should handle refetch errors', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

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
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

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

            // Change to template with same target as current path — no probe needed
            const template2 = mockHalFormsTemplate({target: '/members/123'});

            rerender({template: template2});

            await waitFor(() => {
                expect(result.current.formData).toEqual(currentResourceData);
            });
        });

        it('should fetch from new target when template changes', async () => {
            const template1 = mockHalFormsTemplate({target: '/events/456'});
            const targetData1 = {id: 2, name: 'Target 1'};
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetData1));

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

            // Change to different target — new OPTIONS probe fires (different URL)
            const template2 = mockHalFormsTemplate({target: '/calendar/789'});
            const targetData2 = {id: 3, name: 'Target 2'};
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetData2));

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
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
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
        it('should pass correct query options to useAuthorizedQuery (OPTIONS + GET both fire)', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

            renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            // Verify both OPTIONS and GET were called
            await waitFor(() => {
                expect(fetchSpy).toHaveBeenCalledTimes(2);
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
        it('should fetch fresh GET data on mount with staleTime: 0 (OPTIONS cached at 10min)', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            // OPTIONS cached for 10min, GET always fresh (staleTime: 0)
            fetchSpy.mockResolvedValue(createOptionsResponse(['GET']));

            const realQueryClient = new QueryClient({
                defaultOptions: {
                    queries: {retry: false, gcTime: 60000},
                },
            });

            const wrapper = ({children}: { children: React.ReactNode }) => (
                <QueryClientProvider client={realQueryClient}>
                    {children}
                </QueryClientProvider>
            );

            // First mount: OPTIONS + GET
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result: result1} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper}
            );

            await waitFor(() => {
                expect(result1.current.formData).toEqual(targetResourceData);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(2);

            // Second mount: OPTIONS is cached (staleTime 10min), GET fires again (staleTime 0)
            fetchSpy.mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result: result2} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper}
            );

            await waitFor(() => {
                expect(result2.current.formData).toEqual(targetResourceData);
            });

            // Total: OPTIONS(1, cached) + GET(1) + GET(1) = 3
            expect(fetchSpy).toHaveBeenCalledTimes(3);

            realQueryClient.clear();
        });
    });

    describe('Edge Cases', () => {
        it('should handle empty string currentPathname', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetResourceData));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, ''),
                {wrapper: createWrapper()}
            );

            // Should probe and then fetch because empty pathname differs from /events/456
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

        it('should handle non-FetchError errors (standard Error instances) from OPTIONS', async () => {
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

        it('should handle template.target change during probe fetch', async () => {
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

            // Should be loading (OPTIONS for /events/456 is in flight)
            expect(result.current.isLoadingTargetData).toBe(true);

            // Change template while first probe is pending
            const template2 = mockHalFormsTemplate({target: '/calendar/789'});
            const targetData2 = {id: 3, name: 'Calendar Data'};
            fetchSpy
                .mockResolvedValueOnce(createOptionsResponse(['GET']))
                .mockResolvedValueOnce(createMockResponse(targetData2));

            rerender({template: template2});

            // Resolve first OPTIONS fetch (should be superseded by template change)
            resolveFirstFetch(createOptionsResponse(['GET']));

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
