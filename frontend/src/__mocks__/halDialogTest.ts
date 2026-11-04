import {vi} from 'vitest';
import {createMockResponse} from './mockFetch.ts';
import type {HalRouteContextValue} from '../contexts/halRouteContext.ts';
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
