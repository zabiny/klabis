import {renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import React from 'react';
import {useEventSettingsTabs, useRootNavigation} from './useRootNavigation';
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
    (globalThis as Record<string, unknown>).fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => data,
        headers: new Headers(),
        clone: function () { return this; },
    } as unknown as Response);
};

afterEach(() => {
    delete (globalThis as Record<string, unknown>).fetch;
    vi.clearAllMocks();
});

describe('useRootNavigation — non-menu rels', () => {
    it('never turns the club key link into a menu item', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'orisClubKey']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data!.map(i => i.rel)).toEqual(['events']);
    });
});

describe('useRootNavigation — section assignment', () => {
    it('assigns the profile link to the personal section so it stays out of the menu', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'profile']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data!.find(i => i.rel === 'profile')?.section).toBe('personal');
    });

    it('assigns the notifications link to the personal section so it stays out of the menu', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'notifications']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data!.find(i => i.rel === 'notifications')?.section).toBe('personal');
    });

    it('assigns main section to events and admin section to training-groups, legalGuardianGroups, event-settings', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'training-groups', 'category-presets', 'legalGuardianGroups']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        const findItem = (rel: string) => items.find(i => i.rel === rel);

        expect(findItem('events')?.section).toBe('main');
        expect(findItem('training-groups')?.section).toBe('admin');
        expect(findItem('event-settings')?.section).toBe('admin');
        expect(findItem('legalGuardianGroups')?.section).toBe('admin');
    });

    it('returns only main-section items when response has no admin rels', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'members', 'calendar']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        expect(items.every(i => i.section === 'main')).toBe(true);
        expect(items.some(i => i.section === 'admin')).toBe(false);
    });

    it('does not offer category-presets menu item when root resource lacks the rel', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'members']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data!.some(i => i.rel === 'category-presets')).toBe(false);
    });

    it('returns only admin-section items when response has only admin rels', async () => {
        mockFetchWithResponse(buildHalResponse(['training-groups', 'category-presets', 'legalGuardianGroups']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        expect(items.every(i => i.section === 'admin')).toBe(true);
        expect(items.some(i => i.section === 'main')).toBe(false);
    });

    it('assigns main section to account rel', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'account']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        expect(items.find(i => i.rel === 'account')?.section).toBe('main');
    });

    it('does NOT include legalGuardianGroups navigation item when HAL link is absent', async () => {
        // Backend omits the legalGuardianGroups link for users without MEMBERS:MANAGE permission.
        // Frontend hook should simply not include the item — no legalGuardianGroups nav item appears.
        mockFetchWithResponse(buildHalResponse(['events', 'members', 'groups']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        expect(items.find(i => i.rel === 'legalGuardianGroups')).toBeUndefined();
    });

    it('assigns admin section to membership-fees rel', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'membership-fees']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        expect(items.find(i => i.rel === 'membership-fees')?.section).toBe('admin');
    });

    it('assigns admin section and label "Synchronizace" to syncOverview rel', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'syncOverview']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const item = result.current.data!.find(i => i.rel === 'syncOverview');
        expect(item?.section).toBe('admin');
        expect(item?.label).toBe('Synchronizace');
    });

    it('assigns label "Členské příspěvky" to membership-fees rel', async () => {
        mockFetchWithResponse(buildHalResponse(['membership-fees']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        expect(items.find(i => i.rel === 'membership-fees')?.label).toBe('Členské příspěvky');
    });

    it('does NOT include membership-fee-types as separate menu entry when absent from root links', async () => {
        // Backend no longer contributes membership-fee-types as a root link after the merge.
        mockFetchWithResponse(buildHalResponse(['events', 'membership-fees']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        expect(items.find(i => i.rel === 'membership-fee-types')).toBeUndefined();
    });

    it('does NOT include fee-selection-campaigns as separate menu entry when absent from root links', async () => {
        // Backend no longer contributes fee-selection-campaigns as a root link after the merge.
        mockFetchWithResponse(buildHalResponse(['events', 'membership-fees']));

        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.data).toBeDefined());

        const items = result.current.data!;
        expect(items.find(i => i.rel === 'fee-selection-campaigns')).toBeUndefined();
    });
});

describe('useRootNavigation — event settings entry', () => {
    const load = async (rels: string[]) => {
        mockFetchWithResponse(buildHalResponse(rels));
        const {result} = renderHook(() => useRootNavigation(), {wrapper: createWrapper()});
        await waitFor(() => expect(result.current.data).toBeDefined());
        return result.current.data!;
    };

    it('merges disciplines, event-types and category-presets into one admin item "Nastavení akcí"', async () => {
        const items = await load(['events', 'disciplines', 'event-types', 'category-presets']);

        const adminItems = items.filter(i => i.section === 'admin');
        expect(adminItems).toHaveLength(1);
        expect(adminItems[0]).toMatchObject({
            rel: 'event-settings',
            label: 'Nastavení akcí',
            href: '/event-settings/disciplines',
        });
    });

    it('targets the first available tab when the discipline link is missing', async () => {
        const items = await load(['event-types', 'category-presets']);

        expect(items.find(i => i.rel === 'event-settings')?.href).toBe('/event-settings/event-types');
    });

    it('creates no event settings item when none of the three links exist', async () => {
        const items = await load(['events', 'training-groups']);

        expect(items.find(i => i.rel === 'event-settings')).toBeUndefined();
    });

    it('keeps the three links out of the menu items', async () => {
        const items = await load(['disciplines', 'event-types', 'category-presets']);

        expect(items.map(i => i.rel)).toEqual(['event-settings']);
    });

    it('places the merged item at the position of the first of the three links', async () => {
        const items = await load(['training-groups', 'event-types', 'legalGuardianGroups']);

        const adminRels = items.filter(i => i.section === 'admin').map(i => i.rel);
        expect(adminRels).toEqual(['training-groups', 'event-settings', 'legalGuardianGroups']);
    });
});

describe('useEventSettingsTabs', () => {
    it('returns the available tab links in tab order with route paths', async () => {
        mockFetchWithResponse(buildHalResponse(['events', 'category-presets', 'disciplines']));

        const {result} = renderHook(() => useEventSettingsTabs(), {wrapper: createWrapper()});
        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data!.map(i => [i.rel, i.href])).toEqual([
            ['disciplines', '/disciplines'],
            ['category-presets', '/category-presets'],
        ]);
    });

    it('returns an empty list when no tab link exists', async () => {
        mockFetchWithResponse(buildHalResponse(['events']));

        const {result} = renderHook(() => useEventSettingsTabs(), {wrapper: createWrapper()});
        await waitFor(() => expect(result.current.data).toBeDefined());

        expect(result.current.data).toEqual([]);
    });
});
