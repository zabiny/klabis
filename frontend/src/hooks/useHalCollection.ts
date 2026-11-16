import type {HalEnvelope, PageMetadata} from '../api';
import {selfHref} from '../api/hateoas';
import {useAuthorizedQuery} from './useAuthorizedFetch';

export class HalCollectionError extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'HalCollectionError';
    }
}

function collectionError(envelope: HalEnvelope, detail: string): HalCollectionError {
    const href = selfHref(envelope) ?? '<no self link>';
    const keys = Object.keys(envelope._embedded ?? {});
    return new HalCollectionError(
        `HAL collection at ${href}: ${detail} (embedded keys: ${keys.length > 0 ? keys.join(', ') : 'none'})`,
    );
}

function rowsOf<Row>(value: unknown, envelope: HalEnvelope, collection: string): Row[] {
    if (!Array.isArray(value)) {
        throw collectionError(envelope, `'${collection}' is not an array`);
    }
    return value as Row[];
}

/**
 * Rows of the envelope's HAL collection. The collection name is inferred from the single
 * `_embedded` key; a missing `_embedded` block is Spring HATEOAS' way of saying "empty".
 * Contract violations throw {@link HalCollectionError} instead of silently rendering nothing.
 */
export function embeddedItems<Row>(envelope: HalEnvelope | undefined, collection?: string): Row[] {
    if (envelope === undefined) return [];

    const embedded = envelope._embedded;
    if (embedded === undefined) return [];

    if (collection !== undefined) {
        if (!(collection in embedded)) {
            throw collectionError(envelope, `'${collection}' is not embedded`);
        }
        return rowsOf<Row>(embedded[collection], envelope, collection);
    }

    const keys = Object.keys(embedded);
    if (keys.length > 1) {
        throw collectionError(envelope, 'several embedded collections and no collection given');
    }
    if (keys.length === 0) return [];
    return rowsOf<Row>(embedded[keys[0]], envelope, keys[0]);
}

export interface UseHalCollectionOptions<Row, T> {
    collection?: string;
    select?: (rows: Row[]) => T[];
    staleTime?: number;
    enabled?: boolean;
}

export interface HalCollectionResult<T> {
    items: T[];
    pageMetadata: PageMetadata | undefined;
    isLoading: boolean;
    error: Error | null;
    refetch: () => void;
}

interface CollectedItems<T> {
    items: T[];
    pageMetadata: PageMetadata | undefined;
}

export function useHalCollection<Row, T = Row>(
    href: string | undefined,
    options: UseHalCollectionOptions<Row, T> = {},
): HalCollectionResult<T> {
    const {collection, select, staleTime, enabled} = options;

    const {data, isLoading, error, refetch} = useAuthorizedQuery<CollectedItems<T>>(href, {
        staleTime,
        enabled,
        select: (raw) => {
            const envelope = raw as HalEnvelope;
            const rows = embeddedItems<Row>(envelope, collection);
            return {
                items: select ? select(rows) : (rows as unknown as T[]),
                pageMetadata: envelope.page,
            };
        },
    });

    return {
        items: data?.items ?? [],
        pageMetadata: data?.pageMetadata,
        isLoading: href !== undefined && isLoading,
        error,
        refetch,
    };
}
