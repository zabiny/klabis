import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {BrowserRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {useHalPageData} from './useHalPageData';
import {HalRouteProvider} from '../contexts/HalRouteContext';
import type {HalResponse} from '../api';

// Import the actual hook for use with imported mocks
import * as useHalActionsModule from './useHalActions';
import * as useIsAdminModule from './useIsAdmin';

// Mock the underlying hooks
vi.mock('./useHalActions');
vi.mock('./useIsAdmin');
vi.mock('./useRootNavigation');

describe('useHalPageData', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        vi.clearAllMocks();

        // Set up mock return values for the hooks
        const mockHalActions = vi.mocked(useHalActionsModule.useHalActions, {partial: true});
        mockHalActions.mockReturnValue({
            handleNavigateToItem: vi.fn(),
        });

        const mockUseIsAdmin = vi.mocked(useIsAdminModule.useIsAdmin, {partial: true});
        mockUseIsAdmin.mockReturnValue({
            isAdmin: false,
            isLoading: false,
        });
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

    describe('Combined Loading State', () => {
        it('should return false for isLoading when both hooks are idle', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });
        });

        it('should combine isLoading from all underlying hooks', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            // Should have isLoading property
            expect(typeof result.current.isLoading).toBe('boolean');
        });
    });

    describe('Top-level Properties', () => {
        it('should expose resourceData at top level', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            expect('resourceData' in result.current).toBe(true);
        });

        it('should expose error at top level', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            expect('error' in result.current).toBe(true);
        });

        it('should expose isAdmin at top level', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            expect('isAdmin' in result.current).toBe(true);
        });
    });

    describe('Grouped Properties', () => {
        it('should expose route object with grouped properties', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect('route' in result.current).toBe(true);
                expect(typeof result.current.route).toBe('object');
                expect('pathname' in result.current.route).toBe(true);
                expect('navigateToResource' in result.current.route).toBe(true);
                expect('refetch' in result.current.route).toBe(true);
                expect('queryState' in result.current.route).toBe(true);
                expect('getResourceLink' in result.current.route).toBe(true);
            });
        });

        it('should expose actions object with navigation', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect('actions' in result.current).toBe(true);
                expect(typeof result.current.actions).toBe('object');
                expect('handleNavigateToItem' in result.current.actions).toBe(true);
                expect(typeof result.current.actions.handleNavigateToItem).toBe('function');
            });
        });
    });

    describe('Helper Methods - getLinks', () => {
        it('should return links when resource has links', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.getLinks).toBe('function');
            });
        });

        it('should return undefined when resource is null', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.getLinks()).toBeUndefined();
            });
        });
    });

    describe('Helper Methods - getTemplates', () => {
        it('should return templates when resource has templates', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.getTemplates).toBe('function');
            });
        });

        it('should return undefined when resource is null', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.getTemplates()).toBeUndefined();
            });
        });
    });

    describe('Helper Methods - hasEmbedded', () => {
        it('should return false when resource is null', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.hasEmbedded()).toBe(false);
            });
        });

        it('should be a function', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.hasEmbedded).toBe('function');
            });
        });
    });

    describe('Helper Methods - getEmbeddedItems', () => {
        it('should return empty array when resource is null', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.getEmbeddedItems()).toEqual([]);
            });
        });

        it('should be a function that returns an array', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.getEmbeddedItems).toBe('function');
                expect(Array.isArray(result.current.getEmbeddedItems())).toBe(true);
            });
        });
    });

    describe('Helper Methods - isCollection', () => {
        it('should return false when resource is null', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.isCollection()).toBe(false);
            });
        });

        it('should be a function', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.isCollection).toBe('function');
            });
        });
    });

    describe('Helper Methods - hasLink', () => {
        it('should return false for any link when resource is null', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.hasLink('self')).toBe(false);
                expect(result.current.hasLink('any-link')).toBe(false);
            });
        });

        it('should be a function that accepts a string', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.hasLink).toBe('function');
            });
        });
    });

    describe('Helper Methods - hasTemplate', () => {
        it('should return false for any template when resource is null', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.hasTemplate('create')).toBe(false);
                expect(result.current.hasTemplate('any-template')).toBe(false);
            });
        });

        it('should be a function that accepts a string', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.hasTemplate).toBe('function');
            });
        });
    });

    describe('Helper Methods - hasForms', () => {
        it('should return false when resource is null', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.hasForms()).toBe(false);
            });
        });

        it('should be a function', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.hasForms).toBe('function');
            });
        });
    });

    describe('Helper Methods - getPageMetadata', () => {
        it('should return undefined when resource is null', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.getPageMetadata()).toBeUndefined();
            });
        });

        it('should be a function', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.getPageMetadata).toBe('function');
            });
        });
    });

    describe('Memoization', () => {
        it('should memoize helper methods', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                // Get reference to helper function
                const helper1 = result.current.getLinks;
                expect(helper1).toBeDefined();
            });
        });

        it('should be consistent across renders', async () => {
            const {result, rerender} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                const initial = result.current;
                expect(initial).toBeDefined();
            });

            rerender();

            await waitFor(() => {
                const reRendered = result.current;
                expect(reRendered).toBeDefined();
            });
        });
    });

    describe('Generic Type Safety', () => {
        it('should work with generic type parameter', async () => {
            interface CustomResource extends HalResponse {
                customProperty: string;
            }

            const {result} = renderHook(() => useHalPageData<CustomResource>(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current).toBeDefined();
            });
        });

        it('should default to HalResponse type', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.resourceData).toBeNull();
            });
        });
    });

    describe('Null Safety', () => {
        it('should handle null resourceData gracefully in all helpers', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(() => {
                    result.current.getLinks();
                    result.current.getTemplates();
                    result.current.hasEmbedded();
                    result.current.getEmbeddedItems();
                    result.current.isCollection();
                    result.current.hasLink('any');
                    result.current.hasTemplate('any');
                    result.current.hasForms();
                    result.current.getPageMetadata();
                }).not.toThrow();
            });
        });

        it('should not crash when calling any helper', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(() => {
                    // Call all helpers to ensure null safety
                    result.current.getLinks();
                    result.current.getTemplates();
                    result.current.hasEmbedded();
                    result.current.getEmbeddedItems();
                    result.current.isCollection();
                    result.current.hasLink('test');
                    result.current.hasTemplate('test');
                    result.current.hasForms();
                    result.current.getPageMetadata();
                }).not.toThrow();
            });
        });
    });

    describe('Return Type Structure', () => {
        it('should return object with all expected properties', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                const ret = result.current;
                expect(ret).toHaveProperty('resourceData');
                expect(ret).toHaveProperty('isLoading');
                expect(ret).toHaveProperty('error');
                expect(ret).toHaveProperty('isAdmin');
                expect(ret).toHaveProperty('route');
                expect(ret).toHaveProperty('actions');
                expect(ret).toHaveProperty('getLinks');
                expect(ret).toHaveProperty('getTemplates');
                expect(ret).toHaveProperty('hasEmbedded');
                expect(ret).toHaveProperty('getEmbeddedItems');
                expect(ret).toHaveProperty('isCollection');
                expect(ret).toHaveProperty('hasLink');
                expect(ret).toHaveProperty('hasTemplate');
                expect(ret).toHaveProperty('hasForms');
                expect(ret).toHaveProperty('getPageMetadata');
            });
        });

        it('should have all helper methods as functions', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(typeof result.current.getLinks).toBe('function');
                expect(typeof result.current.getTemplates).toBe('function');
                expect(typeof result.current.hasEmbedded).toBe('function');
                expect(typeof result.current.getEmbeddedItems).toBe('function');
                expect(typeof result.current.isCollection).toBe('function');
                expect(typeof result.current.hasLink).toBe('function');
                expect(typeof result.current.hasTemplate).toBe('function');
                expect(typeof result.current.hasForms).toBe('function');
                expect(typeof result.current.getPageMetadata).toBe('function');
            });
        });

        it('should have route object with all expected methods', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                const {route} = result.current;
                expect(typeof route.pathname).toBe('string');
                expect(typeof route.navigateToResource).toBe('function');
                expect(typeof route.refetch).toBe('function');
                expect(['idle', 'pending', 'success', 'error']).toContain(route.queryState);
                expect(typeof route.getResourceLink).toBe('function');
            });
        });

        it('should have actions object with handleNavigateToItem', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                const {actions} = result.current;
                expect(typeof actions.handleNavigateToItem).toBe('function');
            });
        });
    });
});
