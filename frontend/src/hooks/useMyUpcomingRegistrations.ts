import {selfHref} from '../api/hateoas';
import {useHalCollection} from './useHalCollection';
import type {EventSummaryRow} from './eventSummaryRow';

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
    events.flatMap(e => {
        const href = selfHref(e);
        if (href === undefined) return [];
        return [{
            selfHref: href,
            name: e.name ?? '',
            eventDate: e.eventDate ?? '',
            location: e.location ?? '',
            editRegistrationTarget: e._templates?.editRegistration?.target,
        }];
    });

export function useMyUpcomingRegistrations(href: string | undefined): UseMyUpcomingRegistrationsResult {
    const {items, refetch} = useHalCollection<EventSummaryRow, UpcomingRegistrationItem>(href, {
        select: toUpcomingRegistrationItems,
    });

    return {items, refetch};
}
