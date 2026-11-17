import {act, renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {authorizedFetch} from '../api/authorizedFetch';
import type {HalEnvelope, PageMetadata} from '../api';
import {embeddedItems, HalCollectionError, useHalCollection} from './useHalCollection';

vi.mock('../api/authorizedFetch', () => ({authorizedFetch: vi.fn()}));

const respond = (body: unknown) => vi.mocked(authorizedFetch).mockResolvedValue({
    headers: new Headers(),
    json: async () => body,
} as Response);

const createWrapper = () => {
    const client = new QueryClient({defaultOptions: {queries: {retry: false}}});
    return ({children}: {children: ReactNode}) => <QueryClientProvider client={client}>{children}</QueryClientProvider>;
};

const envelope = (embedded?: Record<string, unknown>, self = '/api/things'): HalEnvelope => ({
    _links: {self: {href: self}},
    ...(embedded === undefined ? {} : {_embedded: embedded}),
});

const page: PageMetadata = {size: 10, totalElements: 2, totalPages: 1, number: 0};

const captureError = (fn: () => unknown): Error => {
    try {
        fn();
    } catch (e) {
        return e as Error;
    }
    throw new Error('expected the call to throw');
};

describe('embeddedItems', () => {
    it('returns no items for an undefined envelope', () => {
        expect(embeddedItems(undefined)).toEqual([]);
    });

    it('returns no items when the envelope has no _embedded block', () => {
        expect(embeddedItems(envelope())).toEqual([]);
    });

    it('returns no items for an empty _embedded block', () => {
        expect(embeddedItems(envelope({}))).toEqual([]);
    });

    it('infers the collection name from the single embedded key', () => {
        expect(embeddedItems<{id: string}>(envelope({thingList: [{id: 'a'}]}))).toEqual([{id: 'a'}]);
    });

    it('throws when several embedded collections are present', () => {
        const response = envelope({members: [{id: 'm'}], others: [{id: 'o'}]}, '/api/groups/1');

        const error = captureError(() => embeddedItems(response));

        expect(error).toBeInstanceOf(HalCollectionError);
        expect(error.message).toContain('/api/groups/1');
        expect(error.message).toContain('members');
        expect(error.message).toContain('others');
    });

    it('throws when the embedded value is not an array', () => {
        const response = envelope({thingList: {id: 'a'}}, '/api/things');

        const error = captureError(() => embeddedItems(response));

        expect(error).toBeInstanceOf(HalCollectionError);
        expect(error.message).toContain('/api/things');
        expect(error.message).toContain('thingList');
    });
});

describe('useHalCollection', () => {
    beforeEach(() => {
        vi.mocked(authorizedFetch).mockReset();
    });

    it('returns typed items and page metadata', async () => {
        respond({_links: {self: {href: '/api/things'}}, _embedded: {thingList: [{id: 'a'}, {id: 'b'}]}, page});

        const {result} = renderHook(() => useHalCollection<{id: string}>('/api/things'), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.items).toEqual([{id: 'a'}, {id: 'b'}]));
        expect(result.current.items[0].id).toBe('a');
        expect(result.current.pageMetadata).toEqual(page);
        expect(result.current.error).toBeNull();
    });

    it('passes typed rows through select', async () => {
        respond({_links: {self: {href: '/api/things'}}, _embedded: {thingList: [{id: 'a'}]}});
        const select = vi.fn((rows: {id: string}[]) => rows.map(row => row.id.toUpperCase()));

        const {result} = renderHook(
            () => useHalCollection<{id: string}, string>('/api/things', {select}),
            {wrapper: createWrapper()},
        );

        await waitFor(() => expect(result.current.items).toEqual(['A']));
        expect(select).toHaveBeenCalledWith([{id: 'a'}]);
    });

    it('reports an empty collection without loading when the href is undefined', () => {
        const {result} = renderHook(() => useHalCollection<{id: string}>(undefined), {wrapper: createWrapper()});

        expect(result.current.items).toEqual([]);
        expect(result.current.isLoading).toBe(false);
        expect(authorizedFetch).not.toHaveBeenCalled();
    });

    it('surfaces a guard failure as the error instead of throwing', async () => {
        respond(envelope({members: [{id: 'm'}], others: [{id: 'o'}]}, '/api/groups/1'));

        const {result} = renderHook(() => useHalCollection<{id: string}>('/api/groups/1'), {wrapper: createWrapper()});

        await waitFor(() => expect(result.current.error).toBeInstanceOf(HalCollectionError));
        expect(result.current.items).toEqual([]);
        expect(result.current.error?.message).toContain('/api/groups/1');
    });

    it('exposes refetch to fetch the collection again', async () => {
        respond({_links: {self: {href: '/api/things'}}, _embedded: {thingList: [{id: 'a'}]}});
        const {result} = renderHook(() => useHalCollection<{id: string}>('/api/things'), {wrapper: createWrapper()});
        await waitFor(() => expect(result.current.items).toHaveLength(1));

        respond({_links: {self: {href: '/api/things'}}, _embedded: {thingList: [{id: 'a'}, {id: 'b'}]}});
        await act(async () => {
            result.current.refetch();
        });

        await waitFor(() => expect(result.current.items).toHaveLength(2));
    });
});
