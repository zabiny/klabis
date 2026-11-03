import {useMemo, useState} from 'react';
import type {HalCollectionResponse} from '../api';
import {parseHalHref, serializeHalHref} from '../api/hateoas';
import type {TablePageData} from '../components/KlabisTable/types';
import {useAuthorizedQuery} from './useAuthorizedFetch';
import {usePersistedState} from './usePersistedState';
import type {SortState} from './useTableSort';

export function useHalCollectionQuery<T>(
    link: {href: string},
    sort: SortState | undefined,
    collectionName?: string,
    defaultRowsPerPage = 10,
) {
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPageState] = usePersistedState('klabis-table-rows-per-page', defaultRowsPerPage);
    const [previousHref, setPreviousHref] = useState(link.href);

    let currentPage = page;
    if (previousHref !== link.href) {
        setPreviousHref(link.href);
        setPage(0);
        currentPage = 0;
    }

    const queryUrl = useMemo(() => {
        const url = parseHalHref(link.href);
        url.searchParams.set('page', `${currentPage}`);
        url.searchParams.set('size', `${rowsPerPage}`);
        url.searchParams.delete('sort');
        if (sort) {
            url.searchParams.append('sort', `${sort.by},${sort.direction}`);
        }
        return serializeHalHref(url);
    }, [link.href, currentPage, rowsPerPage, sort]);

    const {data: response, error} = useAuthorizedQuery<HalCollectionResponse>(queryUrl, {
        staleTime: 30000,
        gcTime: 1000 * 60 * 5,
        retry: 1,
    });

    const rows = useMemo(() => {
        if (!response) return [] as T[];
        if (collectionName && response._embedded) {
            return (response._embedded[collectionName] as T[]) || [];
        }
        return (response.content as T[]) || (response.data as T[]) || [];
    }, [response, collectionName]);

    const pageData = useMemo(() => (response?.page ? (response.page as TablePageData) : undefined), [response?.page]);

    const setRowsPerPage = (size: number) => {
        setRowsPerPageState(size);
        setPage(0);
    };

    return {rows, pageData, error, page: currentPage, setPage, rowsPerPage, setRowsPerPage};
}
