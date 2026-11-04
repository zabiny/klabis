import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {BrowserRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it} from 'vitest';
import {useHalPageData} from './useHalPageData';
import {HalRouteProvider} from '../contexts/HalRouteContext';
import type {HalResponse} from '../api';

describe('useHalPageData', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
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

    describe('Loading State', () => {
        it('should return false for isLoading when idle', async () => {
            const {result} = renderHook(() => useHalPageData(), {wrapper: createWrapper()});

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });
        });

        it('should expose isLoading as boolean', () => {
            const {result} = renderHook(() => useHalPageData(), {wrapper: createWrapper()});

            expect(typeof result.current.isLoading).toBe('boolean');
        });
    });

    describe('Flat interface', () => {
        it('should expose resourceData and error', () => {
            const {result} = renderHook(() => useHalPageData(), {wrapper: createWrapper()});

            expect('resourceData' in result.current).toBe(true);
            expect('error' in result.current).toBe(true);
        });

        it('should expose route members at top level', async () => {
            const {result} = renderHook(() => useHalPageData(), {wrapper: createWrapper()});

            await waitFor(() => {
                expect(typeof result.current.navigateToResource).toBe('function');
                expect(typeof result.current.refetch).toBe('function');
                expect(typeof result.current.getResourceLink).toBe('function');
            });
        });

        it('should not expose removed members', () => {
            const {result} = renderHook(() => useHalPageData(), {wrapper: createWrapper()});

            for (const removed of ['route', 'actions', 'isAdmin', 'queryState', 'getLinks', 'getTemplates',
                'hasEmbedded', 'getEmbeddedItems', 'isCollection', 'hasLink', 'hasTemplate', 'hasForms',
                'getPageMetadata']) {
                expect(removed in result.current).toBe(false);
            }
        });

        it('should return exactly the narrowed set of properties', () => {
            const {result} = renderHook(() => useHalPageData(), {wrapper: createWrapper()});

            expect(Object.keys(result.current).sort()).toEqual([
                'error', 'getResourceLink', 'isLoading', 'navigateToResource', 'refetch', 'resourceData',
            ]);
        });
    });

    describe('Null safety', () => {
        it('should return null resourceData and null link when no resource is loaded', async () => {
            const {result} = renderHook(() => useHalPageData(), {wrapper: createWrapper()});

            await waitFor(() => {
                expect(result.current.resourceData).toBeNull();
            });
            expect(result.current.getResourceLink()).toBeNull();
        });
    });

    describe('Generic type parameter', () => {
        it('should accept a specific resource type', () => {
            interface TestResource extends HalResponse {
                id: string;
            }

            const {result} = renderHook(() => useHalPageData<TestResource>(), {wrapper: createWrapper()});

            expect('resourceData' in result.current).toBe(true);
        });
    });
});
