import {useHalCollection} from './useHalCollection.ts';

export interface EventTypeCatalogItem {
    id: string;
    name: string;
    color?: string;
    sortOrder: number;
}

interface UseEventTypesResult {
    eventTypes: EventTypeCatalogItem[];
    isLoading: boolean;
    getById: (id: string | null | undefined) => EventTypeCatalogItem | undefined;
}

/**
 * Fetches and caches the event types catalog from /api/event-types.
 * Used across events list, event detail and event form for badge display and dropdown options.
 */
export function useEventTypes(): UseEventTypesResult {
    const {items: eventTypes, isLoading} = useHalCollection<EventTypeCatalogItem>('/api/event-types', {
        staleTime: 5 * 60 * 1000,
    });

    const getById = (id: string | null | undefined): EventTypeCatalogItem | undefined => {
        if (!id) return undefined;
        return eventTypes.find((t) => t.id === id);
    };

    return {eventTypes, isLoading, getById};
}
