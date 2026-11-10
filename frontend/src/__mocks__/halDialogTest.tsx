/* eslint-disable react-refresh/only-export-components */
import {QueryClient} from '@tanstack/react-query';
import {useLocation} from 'react-router-dom';
import {vi} from 'vitest';
import {createMockResponse} from './mockFetch.ts';
import type {HalRouteContextValue} from '../contexts/halRouteContext.ts';
import {useToast} from '../contexts/toastContext.ts';
import type {HalResponse} from '../api';

export const halRouteValue = (resourceData: HalResponse | null, pathname: string): HalRouteContextValue => ({
    resourceData,
    navigateToResource: vi.fn(),
    isLoading: false,
    error: null,
    refetch: async () => {
    },
    pathname,
    queryState: 'success',
    getResourceLink: vi.fn(),
});

export const stubFetch = (respond: (url: string, init?: RequestInit) => unknown = () => ({})) => {
    const fetchSpy = vi.fn(async (url: string, init?: RequestInit) => {
        if (init?.method === 'OPTIONS') return createMockResponse(null, 200);
        return createMockResponse(respond(url, init), 200);
    });
    vi.stubGlobal('fetch', fetchSpy);
    const writeCalls = () => fetchSpy.mock.calls.filter(([, init]) => {
        const method = (init as RequestInit | undefined)?.method;
        return method !== undefined && method !== 'OPTIONS' && method !== 'GET';
    });
    return {fetchSpy, writeCalls};
};

export const ToastProbe = () => {
    const {toasts} = useToast();
    return <ul>{toasts.map(t => <li key={t.id} data-testid="toast">{t.message}</li>)}</ul>;
};

export const LocationProbe = () => <span data-testid="location">{useLocation().pathname}</span>;

export const createTestQueryClient = () => new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
