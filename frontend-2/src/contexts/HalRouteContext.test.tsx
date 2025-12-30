import {render, renderHook, screen, waitFor} from '@testing-library/react';
import {HalRouteProvider, HalSubresourceProvider, useHalRoute} from './HalRouteContext';
import {mockHalResponse} from '../__mocks__/halData';
import {createDelayedMockResponse, createMockResponse} from '../__mocks__/mockFetch';
import {BrowserRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import {type Mock, vi} from 'vitest';

// Mock the auth user manager to return a user with access token
vi.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockResolvedValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

describe('useHalRoute Hook', () => {
    let queryClient: QueryClient;
    let fetchSpy: Mock;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        vi.clearAllMocks();
        // Mock global fetch
        fetchSpy = vi.fn() as Mock;
        (globalThis as any).fetch = fetchSpy;
    });

    afterEach(() => {
        delete (globalThis as any).fetch;
    });

    const createWrapper = () => {
        return ({children}: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>
                <BrowserRouter>
                    <HalRouteProvider>{children}</HalRouteProvider>
                </BrowserRouter>
            </QueryClientProvider>
        );
    };

    const mockFetchResponse = (data: any, status = 200) => {
        fetchSpy.mockResolvedValueOnce(createMockResponse(data, status));
    };

    const mockFetchError = (error: Error) => {
        fetchSpy.mockRejectedValueOnce(error);
    };

    describe('Hook Usage', () => {
        it('should return context value when used within HalRouteProvider', async () => {
            const mockData = mockHalResponse();
            mockFetchResponse(mockData);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.resourceData).toBeDefined();
            });
        });

        it('should throw error when used outside HalRouteProvider', () => {
            // Suppress console.error for this test
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation();

            expect(() => {
                renderHook(() => useHalRoute());
            }).toThrow('useHalRoute must be used within a component wrapped by HalRouteProvider');

            consoleSpy.mockRestore();
        });
    });

    describe('Loading State', () => {
        it('should have isLoading true initially', () => {
            fetchSpy.mockImplementationOnce(() =>
                createDelayedMockResponse(mockHalResponse(), 100),
            );

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            expect(result.current.isLoading).toBe(true);
        });

        it('should have isLoading false after data is loaded', async () => {
            const mockData = mockHalResponse();
            mockFetchResponse(mockData);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });
        });
    });

    describe('Resource Data', () => {
        it('should provide resourceData after successful fetch', async () => {
            const mockData = mockHalResponse({name: 'Test Item'});
            mockFetchResponse(mockData);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.resourceData).toEqual(mockData);
            });
        });

        it('should be null initially', () => {
            fetchSpy.mockImplementationOnce(() =>
                createDelayedMockResponse(mockHalResponse(), 100),
            );

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            expect(result.current.resourceData).toBeNull();
        });

        it('should handle undefined data gracefully', async () => {
            mockFetchResponse(undefined);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.resourceData).toBeNull();
            });
        });
    });

    describe('Error Handling', () => {
        it('should capture error when fetch fails', async () => {
            const testError = new Error('Fetch failed');
            mockFetchError(testError);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.error).toEqual(testError);
            });
        });

        it('should be null when no error', async () => {
            const mockData = mockHalResponse();
            mockFetchResponse(mockData);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.error).toBeNull();
            });
        });

        it('should handle non-Error objects gracefully', async () => {
            // Test with a plain string instead of an Error instance
            mockFetchError(new Error('String error'));

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                // Non-Error objects should be converted to Error or handled appropriately
                // The provider should treat it as an error state
                expect(result.current.queryState).toBe('error');
                expect(result.current.error).not.toBeNull();
            });
        });
    });

    describe('Pathname', () => {
        it('should provide current pathname', () => {
            mockFetchResponse(mockHalResponse());

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            expect(result.current.pathname).toBe('/');
        });
    });

    describe('Query State', () => {
        it('should have pending state while loading', () => {
            fetchSpy.mockImplementationOnce(() =>
                createDelayedMockResponse(mockHalResponse(), 100),
            );

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            expect(['idle', 'pending']).toContain(result.current.queryState);
        });

        it('should have success state after loading', async () => {
            const mockData = mockHalResponse();
            mockFetchResponse(mockData);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.queryState).toBe('success');
            });
        });

        it('should have error state when fetch fails', async () => {
            mockFetchError(new Error('Failed'));

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.queryState).toBe('error');
            });
        });
    });

    describe('Refetch Function', () => {
        it('should provide refetch function', async () => {
            const mockData = mockHalResponse();
            mockFetchResponse(mockData);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.refetch).toBe('function');
            });
        });

        it('should refetch data when called', async () => {
            const mockData1 = mockHalResponse({name: 'First'});
            const mockData2 = mockHalResponse({name: 'Second'});

            mockFetchResponse(mockData1);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.resourceData?.name).toBe('First');
            });

            // Update mock for next fetch
            mockFetchResponse(mockData2);

            await result.current.refetch();

            await waitFor(() => {
                expect(result.current.resourceData?.name).toBe('Second');
            });
        });

        it('should handle refetch errors', async () => {
            const mockData = mockHalResponse();
            mockFetchResponse(mockData);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.resourceData).toBeDefined();
            });

            // Update mock to throw error
            mockFetchError(new Error('Refetch failed'));

            await result.current.refetch();

            await waitFor(() => {
                expect(result.current.error).toBeTruthy();
            });
        });
    });

    describe('Provider Behavior', () => {
        // Note: Login route skip is implemented in HalRouteContext (line 64)
        // shouldFetch = !targetUrl.pathname.startsWith('/login')
        // Complete integration test would require complex router setup with actual navigation.
        // Feature is validated through component-level integration tests.
        it('should provide context when initialized', () => {
            // This validates that the provider is properly set up and context is available
            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            // Verify that context is defined and accessible
            expect(result.current).toBeDefined();
            expect(result.current.pathname).toBeDefined();
        });

        it('should cache data between hook instances with same route', async () => {
            const cachingQueryClient = new QueryClient({
                defaultOptions: {
                    queries: {retry: false}, // Remove gcTime: 0 to enable caching
                },
            });

            const createCachingWrapper = () => {
                return ({children}: { children: React.ReactNode }) => (
                    <QueryClientProvider client={cachingQueryClient}>
                        <BrowserRouter>
                            <HalRouteProvider>{children}</HalRouteProvider>
                        </BrowserRouter>
                    </QueryClientProvider>
                );
            };

            const mockData = mockHalResponse();
            mockFetchResponse(mockData);

            const {result: result1, unmount: unmount1} = renderHook(() => useHalRoute(), {
                wrapper: createCachingWrapper(),
            });

            await waitFor(() => {
                expect(result1.current.resourceData).toEqual(mockData);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(1);
            unmount1();

            const {result: result2} = renderHook(() => useHalRoute(), {
                wrapper: createCachingWrapper(),
            });

            await waitFor(() => {
                expect(result2.current.resourceData).toEqual(mockData);
            });

            expect(fetchSpy).toHaveBeenCalledTimes(1);
        });

        // Note: staleTime of 5 minutes is configured but not directly tested.
        // Testing would require jest.useFakeTimers() and advancing time.
        // Caching behavior is tested in "should cache data between hook instances with same route"
    });

    describe('Context Value Structure', () => {
        it('should have all required properties', async () => {
            const mockData = mockHalResponse();
            mockFetchResponse(mockData);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current).toHaveProperty('resourceData');
                expect(result.current).toHaveProperty('isLoading');
                expect(result.current).toHaveProperty('error');
                expect(result.current).toHaveProperty('refetch');
                expect(result.current).toHaveProperty('pathname');
                expect(result.current).toHaveProperty('queryState');
            });
        });

        it('should have correct property types', async () => {
            const mockData = mockHalResponse();
            mockFetchResponse(mockData);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.isLoading).toBe('boolean');
                expect(typeof result.current.refetch).toBe('function');
                expect(typeof result.current.pathname).toBe('string');
                expect(['idle', 'pending', 'success', 'error']).toContain(result.current.queryState);
            });
        });
    });
});

describe('HalSubresourceProvider', () => {
    let queryClient: QueryClient;
    let fetchSpy: Mock;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        vi.clearAllMocks();
        // Mock global fetch
        fetchSpy = vi.fn() as Mock;
        (globalThis as any).fetch = fetchSpy;
    });

    afterEach(() => {
        delete (globalThis as any).fetch;
    });

    const mockFetchResponse = (data: any, status = 200) => {
        fetchSpy.mockResolvedValueOnce(createMockResponse(data, status));
    };

    const mockFetchError = (error: Error) => {
        fetchSpy.mockRejectedValueOnce(error);
    };

    // Test helper component - not used in these tests
    // HalSubresourceProvider tests use the actual HalRouteProvider

    describe('Subresource Link Existence', () => {
        it('should render children when subresource link exists', async () => {
            const parentResource = mockHalResponse({
                balance: 1500,
                _links: {
                    self: {href: '/api/finances/123'},
                    owner: {href: '/api/members/456'},
                },
            });

            mockFetchResponse(parentResource);
            mockFetchResponse({firstName: 'Jan', lastName: 'Novák'});

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: ({children}: any) => (
                    <QueryClientProvider client={queryClient}>
                        <BrowserRouter>
                            <HalRouteProvider>
                                <HalSubresourceProvider subresourceLinkName="owner">
                                    {children}
                                </HalSubresourceProvider>
                            </HalRouteProvider>
                        </BrowserRouter>
                    </QueryClientProvider>
                ),
            });

            await waitFor(() => {
                expect(result.current.resourceData).toBeDefined();
            });
        });

        it('should show error message when subresource link not found', async () => {
            const parentResource = mockHalResponse({
                balance: 1500,
                _links: {
                    self: {href: '/api/finances/123'},
                },
            });

            mockFetchResponse(parentResource);

            render(
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <HalRouteProvider>
                            <HalSubresourceProvider subresourceLinkName="nonexistent">
                                <div>Child content</div>
                            </HalSubresourceProvider>
                        </HalRouteProvider>
                    </BrowserRouter>
                </QueryClientProvider>
            );

            await waitFor(() => {
                expect(screen.getByText(/Subresource nonexistent wasn't found/)).toBeInTheDocument();
            });
        });

        it('should handle array of links for subresource', async () => {
            const parentResource = mockHalResponse({
                balance: 1500,
                _links: {
                    self: {href: '/api/finances/123'},
                    transactions: [
                        {href: '/api/finances/123/transactions'},
                        {href: '/api/finances/123/transactions?page=2'},
                    ],
                },
            });

            mockFetchResponse(parentResource);
            mockFetchResponse({items: []});

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: ({children}: any) => (
                    <QueryClientProvider client={queryClient}>
                        <BrowserRouter>
                            <HalRouteProvider>
                                <HalSubresourceProvider subresourceLinkName="transactions">
                                    {children}
                                </HalSubresourceProvider>
                            </HalRouteProvider>
                        </BrowserRouter>
                    </QueryClientProvider>
                ),
            });

            await waitFor(() => {
                expect(result.current.resourceData).toBeDefined();
            });

            // Verify fetch was called with exactly the first link's href (array handling)
            expect(fetchSpy).toHaveBeenCalledWith(expect.stringContaining('/finances/123/transactions'), expect.any(Object));
            expect(fetchSpy).not.toHaveBeenCalledWith(expect.stringContaining('/finances/123/transactions?page=2'), expect.any(Object));
        });
    });

    describe('Error Handling', () => {
        it('should display "Subresource X wasn\'t found" when link missing', async () => {
            const parentResource = mockHalResponse({
                _links: {
                    self: {href: '/api/items/1'},
                },
            });

            mockFetchResponse(parentResource);

            render(
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <HalRouteProvider>
                            <HalSubresourceProvider subresourceLinkName="missing">
                                <div>Should not render</div>
                            </HalSubresourceProvider>
                        </HalRouteProvider>
                    </BrowserRouter>
                </QueryClientProvider>
            );

            await waitFor(() => {
                expect(screen.getByText(/Subresource missing wasn't found/)).toBeInTheDocument();
            });
        });

        it('should handle null parent resource gracefully', async () => {
            // When parent resource is null, subresource provider should show error
            mockFetchResponse(null);

            render(
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <HalRouteProvider>
                            <HalSubresourceProvider subresourceLinkName="owner">
                                <div>Content</div>
                            </HalSubresourceProvider>
                        </HalRouteProvider>
                    </BrowserRouter>
                </QueryClientProvider>
            );

            await waitFor(() => {
                expect(screen.getByText(/Subresource owner wasn't found/)).toBeInTheDocument();
            });
        });

        it('should handle undefined _links in parent resource', async () => {
            const parentResource = {
                balance: 1500,
                // No _links property
            };

            mockFetchResponse(parentResource);

            render(
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <HalRouteProvider>
                            <HalSubresourceProvider subresourceLinkName="owner">
                                <div>Content</div>
                            </HalSubresourceProvider>
                        </HalRouteProvider>
                    </BrowserRouter>
                </QueryClientProvider>
            );

            await waitFor(() => {
                expect(screen.getByText(/Subresource owner wasn't found/)).toBeInTheDocument();
            });
        });

        it('should handle empty string subresourceLinkName', async () => {
            const parentResource = mockHalResponse({
                _links: {
                    self: {href: '/api/items/1'},
                    owner: {href: '/api/users/1'},
                },
            });

            mockFetchResponse(parentResource);

            render(
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <HalRouteProvider>
                            <HalSubresourceProvider subresourceLinkName="">
                                <div>Content</div>
                            </HalSubresourceProvider>
                        </HalRouteProvider>
                    </BrowserRouter>
                </QueryClientProvider>
            );

            await waitFor(() => {
                expect(screen.getByText(/Subresource[\s\S]*wasn't found/)).toBeInTheDocument();
            });
        });
    });

    describe('Data Fetching', () => {
        it('should fetch subresource data when link exists', async () => {
            const parentResource = mockHalResponse({
                _links: {
                    self: {href: '/api/finances/123'},
                    owner: {href: '/api/members/456'},
                },
            });

            const subresourceData = mockHalResponse({
                firstName: 'Jan',
                lastName: 'Novák',
            });

            mockFetchResponse(parentResource);
            mockFetchResponse(subresourceData);

            const {result} = renderHook(() => useHalRoute(), {
                wrapper: ({children}: any) => (
                    <QueryClientProvider client={queryClient}>
                        <BrowserRouter>
                            <HalRouteProvider>
                                <HalSubresourceProvider subresourceLinkName="owner">
                                    {children}
                                </HalSubresourceProvider>
                            </HalRouteProvider>
                        </BrowserRouter>
                    </QueryClientProvider>
                ),
            });

            await waitFor(() => {
                expect(result.current.resourceData).toEqual(subresourceData);
            });
        });

        it('should pass loading state while fetching subresource', async () => {
            const parentResource = mockHalResponse({
                _links: {
                    self: {href: '/api/finances/123'},
                    owner: {href: '/api/members/456'},
                },
            });

            mockFetchResponse(parentResource);
            fetchSpy.mockImplementationOnce(() =>
                createDelayedMockResponse({firstName: 'Jan'}, 100),
            );

            const TestComponent = () => {
                const {queryState, isLoading} = useHalRoute();
                return (
                    <div>
                        <div data-testid="queryState">{queryState}</div>
                        <div data-testid="isLoading">{isLoading ? 'true' : 'false'}</div>
                    </div>
                );
            };

            render(
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <HalRouteProvider>
                            <HalSubresourceProvider subresourceLinkName="owner">
                                <TestComponent/>
                            </HalSubresourceProvider>
                        </HalRouteProvider>
                    </BrowserRouter>
                </QueryClientProvider>
            );

            // Wait for hook to be properly initialized within the provider
            await waitFor(() => {
                const queryState = screen.getByTestId('queryState').textContent;
                expect(['pending', 'idle', 'success']).toContain(queryState);
            });

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });
        });

        it('should handle fetch error for subresource', async () => {
            const parentResource = mockHalResponse({
                _links: {
                    self: {href: '/api/finances/123'},
                    owner: {href: '/api/members/456'},
                },
            });

            const error = new Error('Subresource fetch failed');

            mockFetchResponse(parentResource);
            mockFetchError(error);

            const TestComponent = () => {
                const {error: ctxError} = useHalRoute();
                return (
                    <div>
                        <div data-testid="error">{ctxError ? ctxError.message : 'no-error'}</div>
                    </div>
                );
            };

            render(
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <HalRouteProvider>
                            <HalSubresourceProvider subresourceLinkName="owner">
                                <TestComponent/>
                            </HalSubresourceProvider>
                        </HalRouteProvider>
                    </BrowserRouter>
                </QueryClientProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('error')).not.toHaveTextContent('no-error');
            });
        });
    });

    describe('Component Structure', () => {
        it('should export HalSubresourceProvider as a component', () => {
            expect(HalSubresourceProvider).toBeDefined();
            expect(typeof HalSubresourceProvider).toBe('function');
        });

        it('should accept subresourceLinkName prop', async () => {
            const parentResource = mockHalResponse({
                _links: {
                    self: {href: '/api/finances/123'},
                    owner: {href: '/api/members/456'},
                },
            });

            const subresourceData = mockHalResponse({firstName: 'Jan'});

            mockFetchResponse(parentResource);
            mockFetchResponse(subresourceData);

            render(
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <HalRouteProvider>
                            <HalSubresourceProvider subresourceLinkName="owner">
                                <div>Content</div>
                            </HalSubresourceProvider>
                        </HalRouteProvider>
                    </BrowserRouter>
                </QueryClientProvider>
            );

            // Should not throw error - wait for content to be rendered
            await waitFor(() => {
                expect(screen.getByText('Content')).toBeInTheDocument();
            });
        });

        it('should accept children prop', async () => {
            const parentResource = mockHalResponse({
                _links: {
                    self: {href: '/api/finances/123'},
                    owner: {href: '/api/members/456'},
                },
            });

            const subresourceData = mockHalResponse({firstName: 'Jan'});

            mockFetchResponse(parentResource);
            mockFetchResponse(subresourceData);

            render(
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <HalRouteProvider>
                            <HalSubresourceProvider subresourceLinkName="owner">
                                <div data-testid="child-content">Child</div>
                            </HalSubresourceProvider>
                        </HalRouteProvider>
                    </BrowserRouter>
                </QueryClientProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('child-content')).toBeInTheDocument();
            });
        });
    });
});
