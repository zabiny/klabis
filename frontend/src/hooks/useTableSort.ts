import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';

export type SortState = { by: string; direction: 'asc' | 'desc' };

function storageKey(tableId: string): string {
    return `klabis.table.${tableId}.sort`;
}

function parseSortString(value: string | null): SortState | null {
    if (!value) return null;
    const commaIndex = value.lastIndexOf(',');
    if (commaIndex <= 0) return null;
    const by = value.substring(0, commaIndex);
    const direction = value.substring(commaIndex + 1);
    if (!by || (direction !== 'asc' && direction !== 'desc')) return null;
    return { by, direction };
}

function readLocalStorage(tableId: string): SortState | null {
    try {
        return parseSortString(localStorage.getItem(storageKey(tableId)));
    } catch {
        return null;
    }
}

function resolveInitialSort(
    urlSortParam: string | null,
    tableId: string,
    defaultSort: SortState,
): SortState {
    const fromUrl = parseSortString(urlSortParam);
    if (fromUrl) return fromUrl;
    const fromStorage = readLocalStorage(tableId);
    if (fromStorage) return fromStorage;
    return defaultSort;
}

export function useTableSort(
    tableId: string,
    defaultSort: SortState,
): {
    sort: SortState;
    setSort: (next: SortState) => void;
    reset: () => void;
} {
    const [searchParams, setSearchParams] = useSearchParams();

    const [sort, setSortState] = useState<SortState>(() =>
        resolveInitialSort(searchParams.get('sort'), tableId, defaultSort),
    );

    const setSort = (next: SortState) => {
        const sortValue = `${next.by},${next.direction}`;
        localStorage.setItem(storageKey(tableId), sortValue);
        setSearchParams(
            (prev) => {
                const next = new URLSearchParams(prev);
                next.set('sort', sortValue);
                return next;
            },
            { replace: true },
        );
        setSortState(next);
    };

    const reset = () => {
        localStorage.removeItem(storageKey(tableId));
        setSearchParams(
            (prev) => {
                const next = new URLSearchParams(prev);
                next.delete('sort');
                return next;
            },
            { replace: true },
        );
        setSortState(defaultSort);
    };

    return { sort, setSort, reset };
}
