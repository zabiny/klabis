import {useMemo, useState} from 'react';
import type {PageMetadata} from '../api';
import {parseHalHref, serializeHalHref} from '../api/hateoas';
import {useHalCollection} from './useHalCollection';
import {usePersistedState} from './usePersistedState';
import type {SortState} from './useTableSort';

export interface HalTableQueryResult<T> {
    items: T[];
    pageMetadata: PageMetadata | undefined;
    error: Error | null;
    currentPage: number;
    setPage: (page: number) => void;
    rowsPerPage: number;
    setRowsPerPage: (size: number) => void;
}

/**
 * Paginated, sorted view of the HAL collection behind `link`. Paging and sorting state is
 * applied to the link's query string; the rows themselves come from {@link useHalCollection}.
 */
export function useHalTableQuery<T>(
    link: {href: string},
    sort: SortState | undefined,
    collectionName?: string,
    defaultRowsPerPage = 10,
): HalTableQueryResult<T> {
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

    const {items, pageMetadata, error} = useHalCollection<T>(queryUrl, {
        collection: collectionName,
        staleTime: 30000,
    });

    const setRowsPerPage = (size: number) => {
        setRowsPerPageState(size);
        setPage(0);
    };

    return {items, pageMetadata, error, currentPage, setPage, rowsPerPage, setRowsPerPage};
}
