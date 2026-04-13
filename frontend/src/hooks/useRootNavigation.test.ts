import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import React from 'react';
import {useRootNavigation} from './useRootNavigation';
import {vi} from 'vitest';

vi.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

const createWrapper = () => {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false, gcTime: 0}},
    });
    return ({children}: {children: ReactNode}) =>
        React.createElement(QueryClientProvider, {client: queryClient}, children);
};

const buildHalResponse = (rels: string[]) => ({
    _links: Object.fromEntries([
        ...rels.map(rel => [rel, {href: `/api/${rel}`, title: rel}]),
    ]),
});

const mockFetchWithResponse = (data: unknown) => {
    (globalThis as any).fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => data,
        headers: new Headers(),
        clone: function () { return this; },
    } as any);
};

afterEach(() => {
    delete (globalThis as any).fetch;
    vi.clearAllMocks();
});

describe('useRootNavigation — section assignment', () => {
    it('assigns main section to events and admin section to training-groups, category-presets, family-groups', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'training-groups', 'category-presets', 'family-groups']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        const findItem = (rel: string) => items.find(i => i.rel === rel);

        expect(findItem('events')?.section).toBe('main');
        expect(findItem('training-groups')?.section).toBe('admin');
        expect(findItem('category-presets')?.section).toBe('admin');
        expect(findItem('family-groups')?.section).toBe('admin');
    });

    it('returns only main-section items when response has no admin rels', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'members', 'calendar']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        expect(items.every(i => i.section === 'main')).toBe(true);
        expect(items.some(i => i.section === 'admin')).toBe(false);
    });

    it('returns only admin-section items when response has only admin rels', async () => {
        mockFetchWithResponse(buildHalResponse(['training-groups', 'category-presets', 'family-groups']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        expect(items.every(i => i.section === 'admin')).toBe(true);
        expect(items.some(i => i.section === 'main')).toBe(false);
    });

    it('does NOT include family-groups navigation item when HAL link is absent', async () => {
        // Backend omits the family-groups link for users without MEMBERS:MANAGE permission.
        // Frontend hook should simply not include the item — no family-groups nav item appears.
        mockFetchWithResponse(buildHalResponse(['events', 'members', 'groups']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        expect(items.find(i => i.rel === 'family-groups')).toBeUndefined();
    });
});
