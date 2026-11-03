import {act, renderHook, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import type {ReactNode} from 'react';
import {vi} from 'vitest';
import {authorizedFetch} from '../api/authorizedFetch';
import {useHalCollectionQuery} from './useHalCollectionQuery';

vi.mock('../api/authorizedFetch', () => ({authorizedFetch: vi.fn()}));

const requestedUrls = () => vi.mocked(authorizedFetch).mock.calls.map(([url]) => String(url));

const render = (initial: {href: string}, sort?: {by: string; direction: 'asc' | 'desc'}) => {
    const client = new QueryClient({defaultOptions: {queries: {retry: false}}});
    const wrapper = ({children}: {children: ReactNode}) => <QueryClientProvider client={client}>{children}</QueryClientProvider>;
    return renderHook(
        ({link}) => useHalCollectionQuery<{id: string}>(link, sort, 'items'),
        {wrapper, initialProps: {link: initial}},
    );
};

describe('useHalCollectionQuery', () => {
    beforeEach(() => {
        localStorage.clear();
        vi.mocked(authorizedFetch).mockReset();
        vi.mocked(authorizedFetch).mockResolvedValue({
            headers: new Headers(),
            json: async () => ({_embedded: {items: [{id: 'a'}]}, page: {size: 10, totalElements: 1, totalPages: 1, number: 0}}),
        } as Response);
    });

    it('requests page, size and sort on top of the link', async () => {
        const {result} = render({href: '/api/things?q=abc'}, {by: 'name', direction: 'desc'});

        await waitFor(() => expect(result.current.rows).toHaveLength(1));
        const url = new URL(requestedUrls()[0], 'http://localhost');
        expect(url.searchParams.get('q')).toBe('abc');
        expect(url.searchParams.get('page')).toBe('0');
        expect(url.searchParams.get('size')).toBe('10');
        expect(url.searchParams.get('sort')).toBe('name,desc');
    });

    it('reads rows from the embedded collection', async () => {
        const {result} = render({href: '/api/things'});

        await waitFor(() => expect(result.current.rows).toEqual([{id: 'a'}]));
        expect(result.current.pageData?.totalElements).toBe(1);
    });

    it('returns to page 0 without requesting the old page when the link changes', async () => {
        const {result, rerender} = render({href: '/api/things?q=a'});
        await waitFor(() => expect(result.current.rows).toHaveLength(1));
        act(() => result.current.setPage(2));
        await waitFor(() => expect(requestedUrls().some(u => u.includes('page=2'))).toBe(true));
        vi.mocked(authorizedFetch).mockClear();

        rerender({link: {href: '/api/things?q=ab'}});

        await waitFor(() => expect(requestedUrls()).toHaveLength(1));
        expect(requestedUrls()[0]).toContain('page=0');
        expect(requestedUrls()[0]).toContain('q=ab');
        expect(result.current.page).toBe(0);
    });

    it('returns to page 0 when the page size changes', async () => {
        const {result} = render({href: '/api/things'});
        await waitFor(() => expect(result.current.rows).toHaveLength(1));
        act(() => result.current.setPage(2));

        act(() => result.current.setRowsPerPage(25));

        await waitFor(() => expect(result.current.page).toBe(0));
        expect(result.current.rowsPerPage).toBe(25);
    });
});
