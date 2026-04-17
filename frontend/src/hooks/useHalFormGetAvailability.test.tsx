import React from 'react';
import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalFormGetAvailability} from './useHalFormGetAvailability';
import {createMockResponse} from '../__mocks__/mockFetch';
import {FetchError} from '../api/authorizedFetch';
import {type Mock, vi} from 'vitest';

vi.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

describe('useHalFormGetAvailability', () => {
    let queryClient: QueryClient;
    let fetchSpy: Mock;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        vi.clearAllMocks();
        fetchSpy = vi.fn() as Mock;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (globalThis as any).fetch = fetchSpy;
    });

    afterEach(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        delete (globalThis as any).fetch;
    });

    const createWrapper = () => {
        return ({children}: {children: React.ReactNode}) => (
            <QueryClientProvider client={queryClient}>
                {children}
            </QueryClientProvider>
        );
    };

    it('returns isGetAllowed: true when Allow header contains GET and POST', async () => {
        fetchSpy.mockResolvedValueOnce(createMockResponse(null, 200, {'Allow': 'GET, POST'}));

        const {result} = renderHook(
            () => useHalFormGetAvailability('/events/456', true),
            {wrapper: createWrapper()}
        );

        await waitFor(() => {
            expect(result.current.isLoading).toBe(false);
        });

        expect(result.current.isGetAllowed).toBe(true);
        expect(result.current.error).toBeNull();
    });

    it('returns isGetAllowed: true when Allow header is lowercase', async () => {
        fetchSpy.mockResolvedValueOnce(createMockResponse(null, 200, {'Allow': 'get, post'}));

        const {result} = renderHook(
            () => useHalFormGetAvailability('/events/456', true),
            {wrapper: createWrapper()}
        );

        await waitFor(() => {
            expect(result.current.isLoading).toBe(false);
        });

        expect(result.current.isGetAllowed).toBe(true);
    });

    it('returns isGetAllowed: true when GET appears among multiple methods in any order', async () => {
        fetchSpy.mockResolvedValueOnce(createMockResponse(null, 200, {'Allow': 'POST, GET, PATCH'}));

        const {result} = renderHook(
            () => useHalFormGetAvailability('/events/456', true),
            {wrapper: createWrapper()}
        );

        await waitFor(() => {
            expect(result.current.isLoading).toBe(false);
        });

        expect(result.current.isGetAllowed).toBe(true);
    });

    it('returns isGetAllowed: false when Allow header does not contain GET', async () => {
        fetchSpy.mockResolvedValueOnce(createMockResponse(null, 200, {'Allow': 'POST, PATCH'}));

        const {result} = renderHook(
            () => useHalFormGetAvailability('/events/456', true),
            {wrapper: createWrapper()}
        );

        await waitFor(() => {
            expect(result.current.isLoading).toBe(false);
        });

        expect(result.current.isGetAllowed).toBe(false);
    });

    it('returns isGetAllowed: false when Allow header is missing on 200 response', async () => {
        fetchSpy.mockResolvedValueOnce(createMockResponse(null, 200));

        const {result} = renderHook(
            () => useHalFormGetAvailability('/events/456', true),
            {wrapper: createWrapper()}
        );

        await waitFor(() => {
            expect(result.current.isLoading).toBe(false);
        });

        expect(result.current.isGetAllowed).toBe(false);
    });

    it('does not fire request when enabled is false', () => {
        const {result} = renderHook(
            () => useHalFormGetAvailability('/events/456', false),
            {wrapper: createWrapper()}
        );

        expect(fetchSpy).not.toHaveBeenCalled();
        expect(result.current.isGetAllowed).toBeUndefined();
        expect(result.current.isLoading).toBe(false);
    });

    it('does not fire request when URL is empty', () => {
        const {result} = renderHook(
            () => useHalFormGetAvailability('', true),
            {wrapper: createWrapper()}
        );

        expect(fetchSpy).not.toHaveBeenCalled();
        expect(result.current.isGetAllowed).toBeUndefined();
        expect(result.current.isLoading).toBe(false);
    });

    it('propagates error and keeps isGetAllowed undefined when OPTIONS fails', async () => {
        const fetchError = new FetchError('HTTP 403 (Forbidden)', 403, 'Forbidden', new Headers());
        fetchSpy.mockRejectedValueOnce(fetchError);

        const {result} = renderHook(
            () => useHalFormGetAvailability('/events/456', true),
            {wrapper: createWrapper()}
        );

        await waitFor(() => {
            expect(result.current.isLoading).toBe(false);
        });

        expect(result.current.isGetAllowed).toBeUndefined();
        expect(result.current.error).toBeTruthy();
    });

    it('uses cached result — calling the hook twice with the same URL results in a single OPTIONS request', async () => {
        const sharedQueryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 60000},
            },
        });
        const wrapper = ({children}: {children: React.ReactNode}) => (
            <QueryClientProvider client={sharedQueryClient}>
                {children}
            </QueryClientProvider>
        );

        fetchSpy.mockResolvedValue(createMockResponse(null, 200, {'Allow': 'GET, POST'}));

        const {result: result1} = renderHook(
            () => useHalFormGetAvailability('/events/456', true),
            {wrapper}
        );

        await waitFor(() => {
            expect(result1.current.isGetAllowed).toBe(true);
        });

        const {result: result2} = renderHook(
            () => useHalFormGetAvailability('/events/456', true),
            {wrapper}
        );

        await waitFor(() => {
            expect(result2.current.isGetAllowed).toBe(true);
        });

        expect(fetchSpy).toHaveBeenCalledTimes(1);

        sharedQueryClient.clear();
    });
});
