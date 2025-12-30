import type {ReactNode} from 'react';
import React from 'react';
import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalFormOptions} from './useHalFormOptions';
import type {HalFormsOption} from '../api';
import {createMockResponse} from '../__mocks__/mockFetch';

// Mock dependencies
jest.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: jest.fn().mockResolvedValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

describe('useHalFormOptions', () => {
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
        return ({children}: { children: ReactNode }) =>
            React.createElement(QueryClientProvider, {client: queryClient}, children);
    };

    describe('Inline Options', () => {
        it('should return inline options immediately with no loading', () => {
            const inlineOptions: HalFormsOption = {
                inline: ['Option 1', 'Option 2', 'Option 3'],
            };

            const {result} = renderHook(() => useHalFormOptions(inlineOptions), {
                wrapper: createWrapper(),
            });

            expect(result.current.isLoading).toBe(false);
            expect(result.current.options).toEqual([
                {value: 'Option 1', label: 'Option 1'},
                {value: 'Option 2', label: 'Option 2'},
                {value: 'Option 3', label: 'Option 3'},
            ]);
        });

        it('should handle inline options with objects containing value and prompt', () => {
            const inlineOptions: HalFormsOption = {
                inline: [
                    {value: 'val1', prompt: 'Display 1'},
                    {value: 'val2', prompt: 'Display 2'},
                ],
            };

            const {result} = renderHook(() => useHalFormOptions(inlineOptions), {
                wrapper: createWrapper(),
            });

            expect(result.current.isLoading).toBe(false);
            expect(result.current.options).toEqual([
                {value: 'val1', label: 'Display 1'},
                {value: 'val2', label: 'Display 2'},
            ]);
        });

        it('should handle inline numeric options', () => {
            const inlineOptions: HalFormsOption = {
                inline: [1, 2, 3],
            };

            const {result} = renderHook(() => useHalFormOptions(inlineOptions), {
                wrapper: createWrapper(),
            });

            expect(result.current.isLoading).toBe(false);
            expect(result.current.options).toEqual([
                {value: '1', label: '1'},
                {value: '2', label: '2'},
                {value: '3', label: '3'},
            ]);
        });

        it('should handle empty inline options', () => {
            const inlineOptions: HalFormsOption = {
                inline: [],
            };

            const {result} = renderHook(() => useHalFormOptions(inlineOptions), {
                wrapper: createWrapper(),
            });

            expect(result.current.isLoading).toBe(false);
            expect(result.current.options).toEqual([]);
        });
    });

    describe('Link Options', () => {
        it('should fetch options from link successfully', async () => {
            const mockData = ['Option 1', 'Option 2'];
            fetchSpy.mockResolvedValueOnce(createMockResponse(mockData));

            const linkOptions: HalFormsOption = {
                link: {href: '/api/form-options'},
            };

            const {result} = renderHook(() => useHalFormOptions(linkOptions), {
                wrapper: createWrapper(),
            });

            expect(result.current.isLoading).toBe(true);

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.options).toEqual([
                {value: 'Option 1', label: 'Option 1'},
                {value: 'Option 2', label: 'Option 2'},
            ]);
            // In test environment (production-like), /api/ prefix is stripped
            expect(fetchSpy).toHaveBeenCalledWith(
                '/form-options',
                expect.objectContaining({
                    headers: expect.objectContaining({
                        'Authorization': expect.stringContaining('Bearer'),
                    }),
                })
            );
        });

        it('should handle error when fetching link options', async () => {
            const mockError = new Error('HTTP 500: Server Error');
            fetchSpy.mockRejectedValue(mockError);

            const linkOptions: HalFormsOption = {
                link: {href: '/api/form-options'},
            };

            const {result} = renderHook(() => useHalFormOptions(linkOptions), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.error).toBeDefined();
            });

            expect(result.current.options).toEqual([]);
        });

        it('should handle non-ok response when fetching options', async () => {
            const fetchError = new Error('HTTP 404: Not Found');
            fetchSpy.mockRejectedValue(fetchError);

            const linkOptions: HalFormsOption = {
                link: {href: '/api/missing-options'},
            };

            const {result} = renderHook(() => useHalFormOptions(linkOptions), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.error).toBeDefined();
            });

            expect(result.current.options).toEqual([]);
        });

        it('should cache fetched options by URL', async () => {
            const mockData = ['Cached Option 1', 'Cached Option 2'];
            fetchSpy.mockResolvedValue(createMockResponse(mockData));

            const linkOptions: HalFormsOption = {
                link: {href: '/api/cached-options'},
            };

            // First hook instance
            const {result: result1, unmount: unmount1} = renderHook(
                () => useHalFormOptions(linkOptions),
                {wrapper: createWrapper()},
            );

            await waitFor(() => {
                expect(result1.current.isLoading).toBe(false);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(1);
            unmount1();

            // Second hook instance with same URL - should use cache
            const {result: result2} = renderHook(() => useHalFormOptions(linkOptions), {
                wrapper: createWrapper(),
            });

            // Should be immediately available from cache
            expect(result2.current.isLoading).toBe(false);
            expect(result2.current.options).toEqual([
                {value: 'Cached Option 1', label: 'Cached Option 1'},
                {value: 'Cached Option 2', label: 'Cached Option 2'},
            ]);

            // Should still only have been called once (cache hit)
            expect(fetchSpy).toHaveBeenCalledTimes(1);
        });

        it('should fetch options again when URL changes', async () => {
            const mockData1 = ['Option 1'];
            const mockData2 = ['Option 2'];

            fetchSpy
                .mockResolvedValueOnce(createMockResponse(mockData1))
                .mockResolvedValueOnce(createMockResponse(mockData2));

            let linkOptions: HalFormsOption = {
                link: {href: '/api/options-1'},
            };

            const {result, rerender} = renderHook(() => useHalFormOptions(linkOptions), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.options).toEqual([{value: 'Option 1', label: 'Option 1'}]);
            expect(fetchSpy).toHaveBeenCalledTimes(1);

            // Change URL
            linkOptions = {link: {href: '/api/options-2'}};
            rerender();

            await waitFor(() => {
                expect(result.current.options).toEqual([{value: 'Option 2', label: 'Option 2'}]);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(2);
        });
    });

    describe('Undefined or Missing Options', () => {
        it('should return empty options when option definition is undefined', () => {
            const {result} = renderHook(() => useHalFormOptions(undefined), {
                wrapper: createWrapper(),
            });

            expect(result.current.isLoading).toBe(false);
            expect(result.current.options).toEqual([]);
            expect(result.current.error).toBeNull();
        });

        it('should return empty options when option definition is empty object', () => {
            const emptyOptions: HalFormsOption = {};

            const {result} = renderHook(() => useHalFormOptions(emptyOptions), {
                wrapper: createWrapper(),
            });

            expect(result.current.isLoading).toBe(false);
            expect(result.current.options).toEqual([]);
            expect(result.current.error).toBeNull();
        });
    });

    describe('Loading States', () => {
        it('should transition from loading to loaded', async () => {
            const mockData = ['Option 1'];
            fetchSpy.mockResolvedValue(createMockResponse(mockData));

            const linkOptions: HalFormsOption = {
                link: {href: '/api/slow-options'},
            };

            const {result} = renderHook(() => useHalFormOptions(linkOptions), {
                wrapper: createWrapper(),
            });

            // Initially loading
            expect(result.current.isLoading).toBe(true);
            expect(result.current.options).toEqual([]);

            // Wait for data to load
            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            // Data should be available
            expect(result.current.options).toEqual([{value: 'Option 1', label: 'Option 1'}]);
            expect(fetchSpy).toHaveBeenCalled();
        });

        it('should handle response that is not JSON', async () => {
            const mockResponse = {
                ok: true,
                json: jest.fn().mockRejectedValue(new Error('Invalid JSON')),
                clone: () => ({
                    text: jest.fn().mockResolvedValue('invalid'),
                }),
            } as any;
            fetchSpy.mockResolvedValue(mockResponse);

            const linkOptions: HalFormsOption = {
                link: {href: '/api/bad-json-options'},
            };

            const {result} = renderHook(() => useHalFormOptions(linkOptions), {
                wrapper: createWrapper(),
            });

            // Wait for the error to be caught and loading to complete
            await waitFor(
                () => {
                    expect(result.current.error).toBeDefined();
                },
                {timeout: 3000}
            );

            expect(result.current.options).toEqual([]);
            expect(fetchSpy).toHaveBeenCalled();
        });
    });

    describe('Integration with React Query', () => {
        it('should use React Query for request deduplication', async () => {
            const mockData = ['Option 1'];
            fetchSpy.mockResolvedValue(createMockResponse(mockData));

            const linkOptions: HalFormsOption = {
                link: {href: '/api/dedup-options'},
            };

            // Render multiple hooks with same URL simultaneously
            const {result: result1} = renderHook(() => useHalFormOptions(linkOptions), {
                wrapper: createWrapper(),
            });
            const {result: result2} = renderHook(() => useHalFormOptions(linkOptions), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result1.current.isLoading).toBe(false);
                expect(result2.current.isLoading).toBe(false);
            });

            // Both should have the same data
            expect(result1.current.options).toEqual(result2.current.options);

            // Should only fetch once due to React Query deduplication
            expect(fetchSpy).toHaveBeenCalledTimes(1);
        });
    });
});
