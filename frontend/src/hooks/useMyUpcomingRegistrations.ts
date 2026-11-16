import {asLinkArray, toHref} from '../api/hateoas';
import type {components} from '../api/klabisApi';
import {useHalCollection} from './useHalCollection';

type EventSummaryRow = components['schemas']['EntityModelEventSummaryDto'];

export interface UpcomingRegistrationItem {
    selfHref: string;
    name: string;
    eventDate: string;
    location: string;
    editRegistrationTarget?: string;
}

export interface UseMyUpcomingRegistrationsResult {
    items: UpcomingRegistrationItem[];
    refetch: () => void;
}

const toUpcomingRegistrationItems = (events: EventSummaryRow[]): UpcomingRegistrationItem[] =>
    events
        .filter(e => asLinkArray(e._links?.self)[0])
        .map(e => ({
            selfHref: toHref(e._links!.self!),
            name: e.name ?? '',
            eventDate: e.eventDate ?? '',
            location: e.location ?? '',
            editRegistrationTarget: e._templates?.editRegistration?.target,
        }));

export function useMyUpcomingRegistrations(href: string | undefined): UseMyUpcomingRegistrationsResult {
    const {items, refetch} = useHalCollection<EventSummaryRow, UpcomingRegistrationItem>(href, {
        select: toUpcomingRegistrationItems,
    });

    return {items, refetch};
}
