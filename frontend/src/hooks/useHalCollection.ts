import {useMemo} from 'react';
import type {HalEnvelope, PageMetadata} from '../api';
import {selfHref} from '../api/hateoas';
import {useAuthorizedQuery} from './useAuthorizedFetch';

export class HalCollectionError extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'HalCollectionError';
    }
}

const EMPTY_ITEMS: never[] = [];

function collectionError(envelope: HalEnvelope, detail: string): HalCollectionError {
    const href = selfHref(envelope) ?? '<no self link>';
    const keys = Object.keys(envelope._embedded ?? {});
    return new HalCollectionError(
        `HAL collection at ${href}: ${detail} (embedded keys: ${keys.length > 0 ? keys.join(', ') : 'none'})`,
    );
}

/**
 * Rows of the envelope's HAL collection. The collection name is inferred from the single
 * `_embedded` key; a missing `_embedded` block is Spring HATEOAS' way of saying "empty".
 * Contract violations throw {@link HalCollectionError} instead of silently rendering nothing.
 */
export function embeddedItems<Row>(envelope: HalEnvelope | undefined): Row[] {
    if (envelope === undefined) return [];

    const embedded = envelope._embedded;
    if (embedded === undefined) return [];

    const keys = Object.keys(embedded);
    if (keys.length > 1) {
        throw collectionError(envelope, 'several embedded collections');
    }
    if (keys.length === 0) return [];

    const value = embedded[keys[0]];
    if (!Array.isArray(value)) {
        throw collectionError(envelope, `'${keys[0]}' is not an array`);
    }
    return value as Row[];
}

export interface UseHalCollectionOptions<Row, T> {
    select?: (rows: Row[]) => T[];
    staleTime?: number;
}

export interface HalCollectionResult<T> {
    items: T[];
    pageMetadata: PageMetadata | undefined;
    isLoading: boolean;
    error: Error | null;
    refetch: () => void;
}

interface DecodedItems<T> {
    items: T[];
    pageMetadata: PageMetadata | undefined;
    error: Error | null;
}

export function useHalCollection<Row, T = Row>(
    href: string | undefined,
    options: UseHalCollectionOptions<Row, T> = {},
): HalCollectionResult<T> {
    const {select, staleTime} = options;

    const query = useAuthorizedQuery<HalEnvelope>(href, {staleTime});

    const decoded = useMemo<DecodedItems<T>>(() => {
        try {
            const rows = embeddedItems<Row>(query.data);
            return {
                items: select ? select(rows) : (rows as unknown as T[]),
                pageMetadata: query.data?.page,
                error: null,
            };
        } catch (e) {
            return {items: EMPTY_ITEMS, pageMetadata: undefined, error: e as Error};
        }
    }, [query.data, select]);

    return {
        items: decoded.items,
        pageMetadata: decoded.pageMetadata,
        isLoading: query.isLoading,
        error: query.error ?? decoded.error,
        refetch: query.refetch,
    };
}
