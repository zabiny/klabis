import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {BrowserRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {useHalPageData} from './useHalPageData';
import {HalRouteProvider} from '../contexts/HalRouteContext';
import {AuthContext} from '../contexts/authContext';
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

        it('should return exactly the narrowed set of properties', () => {
            const {result} = renderHook(() => useHalPageData(), {wrapper: createWrapper()});

            expect(Object.keys(result.current).sort()).toEqual([
                'error', 'getResourceLink', 'isLoading', 'navigateToResource', 'queryState', 'refetch', 'resourceData',
            ]);
        });
    });

    describe('Query State', () => {
        it('should expose queryState next to resourceData, isLoading and error', async () => {
            const {result} = renderHook(() => useHalPageData(), {wrapper: createWrapper()});

            await waitFor(() => {
                expect(result.current).toHaveProperty('queryState');
                expect(['idle', 'pending', 'success', 'error']).toContain(result.current.queryState);
            });
        });

        it('should be idle while authentication is not ready', async () => {
            const {result} = renderHook(() => useHalPageData(), {
                wrapper: ({children}: { children: React.ReactNode }) => (
                    <QueryClientProvider client={queryClient}>
                        <BrowserRouter>
                            <AuthContext.Provider
                                value={{
                                    isAuthenticated: false,
                                    isLoading: true,
                                    login: vi.fn(),
                                    logout: vi.fn(),
                                    getUser: () => null
                                }}>
                                <HalRouteProvider>{children}</HalRouteProvider>
                            </AuthContext.Provider>
                        </BrowserRouter>
                    </QueryClientProvider>
                ),
            });

            await waitFor(() => {
                expect(result.current.queryState).toBe('idle');
            });
            expect(result.current.resourceData).toBeNull();
            expect(result.current.isLoading).toBe(false);
            expect(result.current.error).toBeNull();
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
