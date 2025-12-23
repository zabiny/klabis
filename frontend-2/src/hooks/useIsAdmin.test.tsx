import {renderHook} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {type ReactNode} from 'react';
import {useIsAdmin} from './useIsAdmin';
import type {NavigationItem} from './useRootNavigation';
import * as rootNavModule from './useRootNavigation';

// Mock the useRootNavigation hook
jest.mock('./useRootNavigation');

const mockUseRootNavigation = rootNavModule.useRootNavigation as jest.MockedFunction<
    typeof rootNavModule.useRootNavigation
>;

describe('useIsAdmin', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {
                    retry: false,
                },
            },
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    const createWrapper = () => {
        return ({children}: { children: ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        );
    };

    it('should return isAdmin true when sourceEvents link exists', () => {
        const navigationItems: NavigationItem[] = [
            {rel: 'members', label: 'Members', href: '/members'},
            {rel: 'sourceEvents', label: 'System Events', href: '/system-events'},
            {rel: 'events', label: 'Events', href: '/events'},
        ];

        mockUseRootNavigation.mockReturnValue({
            data: navigationItems,
            isLoading: false,
            isError: false,
            error: null,
            status: 'success',
            fetchStatus: 'idle',
            isFetched: true,
            isStale: false,
            isFetching: false,
            isPlaceholderData: false,
            isPending: false,
            isRefetching: false,
            refetch: jest.fn(),
            failureCount: 0,
            failureReason: null,
            errorUpdateCount: 0,
            errorUpdatedAt: null,
            dataUpdatedAt: Date.now(),
        } as any);

        const {result} = renderHook(() => useIsAdmin(), {
            wrapper: createWrapper(),
        });

        expect(result.current.isAdmin).toBe(true);
        expect(result.current.isLoading).toBe(false);
    });

    it('should return isAdmin false when sourceEvents link does not exist', () => {
        const navigationItems: NavigationItem[] = [
            {rel: 'members', label: 'Members', href: '/members'},
            {rel: 'events', label: 'Events', href: '/events'},
        ];

        mockUseRootNavigation.mockReturnValue({
            data: navigationItems,
            isLoading: false,
            isError: false,
            error: null,
            status: 'success',
            fetchStatus: 'idle',
            isFetched: true,
            isStale: false,
            isFetching: false,
            isPlaceholderData: false,
            isPending: false,
            isRefetching: false,
            refetch: jest.fn(),
            failureCount: 0,
            failureReason: null,
            errorUpdateCount: 0,
            errorUpdatedAt: null,
            dataUpdatedAt: Date.now(),
        } as any);

        const {result} = renderHook(() => useIsAdmin(), {
            wrapper: createWrapper(),
        });

        expect(result.current.isAdmin).toBe(false);
        expect(result.current.isLoading).toBe(false);
    });

    it('should return isLoading true when hook is loading', () => {
        mockUseRootNavigation.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
            error: null,
            status: 'pending',
            fetchStatus: 'fetching',
            isFetched: false,
            isStale: false,
            isFetching: true,
            isPlaceholderData: false,
            isPending: true,
            isRefetching: false,
            refetch: jest.fn(),
            failureCount: 0,
            failureReason: null,
            errorUpdateCount: 0,
            errorUpdatedAt: null,
            dataUpdatedAt: 0,
        } as any);

        const {result} = renderHook(() => useIsAdmin(), {
            wrapper: createWrapper(),
        });

        expect(result.current.isLoading).toBe(true);
    });

    it('should return isAdmin false when data is empty array', () => {
        mockUseRootNavigation.mockReturnValue({
            data: [],
            isLoading: false,
            isError: false,
            error: null,
            status: 'success',
            fetchStatus: 'idle',
            isFetched: true,
            isStale: false,
            isFetching: false,
            isPlaceholderData: false,
            isPending: false,
            isRefetching: false,
            refetch: jest.fn(),
            failureCount: 0,
            failureReason: null,
            errorUpdateCount: 0,
            errorUpdatedAt: null,
            dataUpdatedAt: Date.now(),
        } as any);

        const {result} = renderHook(() => useIsAdmin(), {
            wrapper: createWrapper(),
        });

        expect(result.current.isAdmin).toBe(false);
        expect(result.current.isLoading).toBe(false);
    });
});
