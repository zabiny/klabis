import React from 'react';
import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalFormData} from './useHalFormData';
import {mockHalFormsTemplate} from '../__mocks__/halData';

// Mock dependencies
jest.mock('../components/HalNavigator/hooks', () => ({
    fetchResource: jest.fn(),
}));

const {fetchResource} = require('../components/HalNavigator/hooks');

describe('useHalFormData Hook', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        jest.clearAllMocks();
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
        it('should return current resource data when no template selected', () => {
            const {result} = renderHook(
                () => useHalFormData(null, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.formData).toEqual(currentResourceData);
            expect(result.current.isLoadingTargetData).toBe(false);
            expect(result.current.targetFetchError).toBeNull();
        });

        it('should not be loading', () => {
            const {result} = renderHook(
                () => useHalFormData(null, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.isLoadingTargetData).toBe(false);
        });

        it('should not have errors', () => {
            const {result} = renderHook(
                () => useHalFormData(null, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.targetFetchError).toBeNull();
        });

        it('should not fetch from API', () => {
            renderHook(
                () => useHalFormData(null, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(fetchResource).not.toHaveBeenCalled();
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

            expect(fetchResource).not.toHaveBeenCalled();
        });
    });

    describe('Target Equals Current Path', () => {
        it('should return current resource data when target equals current path', () => {
            const template = mockHalFormsTemplate({target: '/members/123'});

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.formData).toEqual(currentResourceData);
        });

        it('should not fetch when target matches current path', () => {
            const template = mockHalFormsTemplate({target: '/members/123'});

            renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(fetchResource).not.toHaveBeenCalled();
        });

        it('should handle /api prefix differences', () => {
            const template = mockHalFormsTemplate({target: '/api/members/123'});

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.formData).toEqual(currentResourceData);
            expect(fetchResource).not.toHaveBeenCalled();
        });
    });

    describe('Target Differs from Current Path', () => {
        it('should fetch data from target when target differs', async () => {
            const template = mockHalFormsTemplate({target: '/api/events/456'});
            fetchResource.mockResolvedValueOnce(targetResourceData);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            // Should be loading initially
            expect(result.current.isLoadingTargetData).toBe(true);

            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });

            expect(fetchResource).toHaveBeenCalledWith('/api/events/456');
            expect(result.current.formData).toEqual(targetResourceData);
        });

        it('should return target data after successful fetch', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchResource.mockResolvedValueOnce(targetResourceData);

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
            fetchResource.mockResolvedValueOnce(targetResourceData);

            renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(fetchResource).toHaveBeenCalledWith('/api/events/456');
            });
        });

        it('should handle full URL targets', async () => {
            const template = mockHalFormsTemplate({
                target: 'https://example.com/api/events/456',
            });
            fetchResource.mockResolvedValueOnce(targetResourceData);

            renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(fetchResource).toHaveBeenCalledWith('/api/events/456');
            });
        });
    });

    describe('Loading States', () => {
        it('should set isLoadingTargetData to true during fetch', () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchResource.mockImplementationOnce(
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
            fetchResource.mockResolvedValueOnce(targetResourceData);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });
        });

        it('should return null formData while loading', () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchResource.mockImplementationOnce(
                () => new Promise((resolve) => setTimeout(resolve, 100))
            );

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.formData).toBeNull();
            expect(result.current.isLoadingTargetData).toBe(true);
        });
    });

    describe('Error Handling', () => {
        it('should set error when fetch fails', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const fetchError = new Error('Network error');
            fetchResource.mockRejectedValueOnce(fetchError);

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
            fetchResource.mockRejectedValueOnce(new Error('Failed'));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.isLoadingTargetData).toBe(false);
            });
        });

        it('should return null formData on error', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchResource.mockRejectedValueOnce(new Error('Failed'));

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.formData).toBeNull();
            });
        });

        it('should preserve error instance', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const fetchError = new Error('Specific error message');
            fetchResource.mockRejectedValueOnce(fetchError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.targetFetchError?.message).toBe('Specific error message');
            });
        });

        it('should return empty data when API returns HTTP 404', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const fetchError = {
                message: 'HTTP 404',
                responseStatus: 404,
                responseStatusText: 'Not Found',
            };
            fetchResource.mockRejectedValueOnce(fetchError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual({});
                expect(result.current.targetFetchError).toBeNull();
            });
        });

        it('should return empty data when API returns HTTP 405', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const fetchError = {
                message: 'HTTP 405',
                responseStatus: 405,
                responseStatusText: 'Method Not Allowed',
            };
            fetchResource.mockRejectedValueOnce(fetchError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual({});
                expect(result.current.targetFetchError).toBeNull();
            });
        });

        it('should still show error for non-404/405 HTTP errors', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            const fetchError = {
                message: 'HTTP 500',
                responseStatus: 500,
                responseStatusText: 'Internal Server Error',
            };
            fetchResource.mockRejectedValueOnce(fetchError);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.targetFetchError).toBeTruthy();
                expect(result.current.formData).toBeNull();
            });
        });
    });

    describe('Refetch Functionality', () => {
        it('should provide refetchTargetData function', () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchResource.mockResolvedValueOnce(targetResourceData);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            expect(result.current.refetchTargetData).toBeInstanceOf(Function);
        });

        it('should refetch data when refetchTargetData is called', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchResource.mockResolvedValueOnce(targetResourceData);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetResourceData);
            });

            expect(fetchResource).toHaveBeenCalledTimes(1);

            // Refetch
            const newTargetData = {id: 3, name: 'Refreshed Data', type: 'target'};
            fetchResource.mockResolvedValueOnce(newTargetData);

            result.current.refetchTargetData();

            await waitFor(() => {
                expect(fetchResource).toHaveBeenCalledTimes(2);
            });

            await waitFor(() => {
                expect(result.current.formData).toEqual(newTargetData);
            });
        });

        it('should handle refetch errors', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchResource.mockResolvedValueOnce(targetResourceData);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetResourceData);
            });

            // Refetch with error
            const refetchError = new Error('Refetch failed');
            fetchResource.mockRejectedValueOnce(refetchError);

            result.current.refetchTargetData();

            await waitFor(() => {
                expect(result.current.targetFetchError).toBeTruthy();
            });
        });
    });

    describe('Template Changes', () => {
        it('should update when template changes', async () => {
            const template1 = mockHalFormsTemplate({target: '/events/456'});
            fetchResource.mockResolvedValueOnce(targetResourceData);

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
            fetchResource.mockResolvedValueOnce(targetData1);

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
            fetchResource.mockResolvedValueOnce(targetData2);

            rerender({template: template2});

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetData2);
            });
        });
    });

    describe('React Query Caching', () => {
        it('should always fetch fresh data (staleTime: 0)', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchResource.mockResolvedValue(targetResourceData);

            // First render
            const {result: result1, unmount} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result1.current.formData).toEqual(targetResourceData);
            });

            expect(fetchResource).toHaveBeenCalledTimes(1);

            // Unmount and remount with same template (should fetch fresh due to staleTime: 0)
            unmount();

            const {result: result2} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result2.current.formData).toEqual(targetResourceData);
            });

            // Should fetch again because staleTime is 0 (always fetch fresh)
            expect(fetchResource).toHaveBeenCalledTimes(2);
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

    describe('Edge Cases', () => {
        it('should handle empty string currentPathname', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchResource.mockResolvedValueOnce(targetResourceData);

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, ''),
                {wrapper: createWrapper()}
            );

            // Should fetch because empty pathname differs from /events/456
            expect(result.current.isLoadingTargetData).toBe(true);

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetResourceData);
            });

            expect(fetchResource).toHaveBeenCalledWith('/api/events/456');
        });

        it('should handle template with empty properties array', () => {
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
            expect(result.current.isLoadingTargetData).toBe(false);
        });

        it('should handle non-Error thrown in fetch', async () => {
            const template = mockHalFormsTemplate({target: '/events/456'});
            fetchResource.mockRejectedValueOnce('String error');

            const {result} = renderHook(
                () => useHalFormData(template, currentResourceData, '/members/123'),
                {wrapper: createWrapper()}
            );

            await waitFor(() => {
                expect(result.current.targetFetchError).toBeTruthy();
            });

            // Should convert string error to Error instance
            expect(result.current.targetFetchError).toBeInstanceOf(Error);
            expect(result.current.targetFetchError?.message).toBe('String error');
        });

        it('should handle template.target change during fetch', async () => {
            const template1 = mockHalFormsTemplate({target: '/events/456'});
            let resolveFirstFetch: (value: unknown) => void;
            const firstFetchPromise = new Promise((resolve) => {
                resolveFirstFetch = resolve;
            });
            fetchResource.mockReturnValueOnce(firstFetchPromise);

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
            fetchResource.mockResolvedValueOnce(targetData2);

            rerender({template: template2});

            // Resolve first fetch (should be ignored due to template change)
            resolveFirstFetch(targetResourceData);

            // Should fetch from new target
            await waitFor(() => {
                expect(fetchResource).toHaveBeenCalledWith('/api/calendar/789');
            });

            await waitFor(() => {
                expect(result.current.formData).toEqual(targetData2);
            });
        });

        it('should handle empty currentResourceData object', () => {
            const emptyData = {};
            const template = mockHalFormsTemplate({target: '/members/123'});

            const {result} = renderHook(
                () => useHalFormData(template, emptyData, '/members/123'),
                {wrapper: createWrapper()}
            );

            // Should return empty object when target matches current path
            expect(result.current.formData).toEqual(emptyData);
            expect(result.current.isLoadingTargetData).toBe(false);
        });
    });
});
