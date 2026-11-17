import {act, renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {authorizedFetch} from '../api/authorizedFetch';
import type {SortState} from './useTableSort';
import {useHalTableQuery} from './useHalTableQuery';

vi.mock('../api/authorizedFetch', () => ({authorizedFetch: vi.fn()}));

const respond = (body: unknown) => vi.mocked(authorizedFetch).mockResolvedValue({
    headers: new Headers(),
    json: async () => body,
} as Response);

const requestedUrls = () => vi.mocked(authorizedFetch).mock.calls.map(([url]) => String(url));

const rows = [{id: 'a'}, {id: 'b'}];
const page = {size: 10, totalElements: 2, totalPages: 1, number: 0};

const render = (
    initial: {href: string},
    {collectionName, sort}: {collectionName?: string; sort?: SortState} = {},
) => {
    const client = new QueryClient({defaultOptions: {queries: {retry: false}}});
    const wrapper = ({children}: {children: ReactNode}) => <QueryClientProvider client={client}>{children}</QueryClientProvider>;
    return renderHook(
        ({link}) => useHalTableQuery<{id: string}>(link, sort, collectionName),
        {wrapper, initialProps: {link: initial}},
    );
};

describe('useHalTableQuery', () => {
    beforeEach(() => {
        localStorage.clear();
        vi.mocked(authorizedFetch).mockReset();
    });

    it('exposes items and page metadata plus the table state, under no legacy names', async () => {
        respond({_embedded: {thingList: rows}, page});

        const {result} = render({href: '/api/things'});

        await waitFor(() => expect(result.current.items).toEqual(rows));
        expect(result.current.pageMetadata).toEqual(page);
        expect(result.current.currentPage).toBe(0);
        expect(result.current.rowsPerPage).toBe(10);
        expect(result.current.setPage).toBeTypeOf('function');
        expect(result.current.setRowsPerPage).toBeTypeOf('function');
        expect(result.current).not.toHaveProperty('rows');
        expect(result.current).not.toHaveProperty('pageData');
        expect(result.current).not.toHaveProperty('page');
    });

    it('requests page, size and sort on top of the link', async () => {
        respond({_embedded: {thingList: rows}, page});

        const {result} = render({href: '/api/things?q=abc'}, {sort: {by: 'name', direction: 'desc'}});

        await waitFor(() => expect(result.current.items).toHaveLength(2));
        const url = new URL(requestedUrls()[0], 'http://localhost');
        expect(url.searchParams.get('q')).toBe('abc');
        expect(url.searchParams.get('page')).toBe('0');
        expect(url.searchParams.get('size')).toBe('10');
        expect(url.searchParams.get('sort')).toBe('name,desc');
    });

    it('honours the explicit collection override', async () => {
        respond({_embedded: {members: [{id: 'm'}], others: [{id: 'o'}]}, page});

        const {result} = render({href: '/api/things'}, {collectionName: 'others'});

        await waitFor(() => expect(result.current.items).toEqual([{id: 'o'}]));
        expect(result.current.error).toBeNull();
    });

    it.each([
        ['no _embedded at all', {page}],
        ['a former content shape', {content: rows, page}],
        ['a former data shape', {data: rows, page}],
    ])('yields empty items for a response with %s', async (_label, body) => {
        respond(body);

        const {result} = render({href: '/api/things'}, {collectionName: 'thingList'});

        await waitFor(() => expect(result.current.pageMetadata).toEqual(page));
        expect(result.current.items).toEqual([]);
        expect(result.current.error).toBeNull();
    });

    it('returns to page 0 without requesting the old page when the link changes', async () => {
        respond({_embedded: {thingList: rows}, page});
        const {result, rerender} = render({href: '/api/things?q=a'});
        await waitFor(() => expect(result.current.items).toHaveLength(2));
        act(() => result.current.setPage(2));
        await waitFor(() => expect(requestedUrls().some(u => u.includes('page=2'))).toBe(true));
        vi.mocked(authorizedFetch).mockClear();

        rerender({link: {href: '/api/things?q=ab'}});

        await waitFor(() => expect(requestedUrls()).toHaveLength(1));
        expect(requestedUrls()[0]).toContain('page=0');
        expect(requestedUrls()[0]).toContain('q=ab');
        expect(result.current.currentPage).toBe(0);
    });

    it('returns to page 0 and persists the size when the page size changes', async () => {
        respond({_embedded: {thingList: rows}, page});
        const {result} = render({href: '/api/things'});
        await waitFor(() => expect(result.current.items).toHaveLength(2));
        act(() => result.current.setPage(2));

        act(() => result.current.setRowsPerPage(25));

        await waitFor(() => expect(result.current.currentPage).toBe(0));
        expect(result.current.rowsPerPage).toBe(25);
        expect(localStorage.getItem('klabis-table-rows-per-page')).toBe('25');
    });
});
