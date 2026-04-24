import type {HalResponse} from '../api';
import {toHref} from '../api/hateoas';
import {useAuthorizedQuery} from './useAuthorizedFetch';

export interface UpcomingRegistrationItem {
    selfHref: string;
    name: string;
    eventDate: string;
    location: string;
}

export interface UpcomingRegistrationsData {
    items: UpcomingRegistrationItem[];
}

function toUpcomingRegistrationsData(response: HalResponse): UpcomingRegistrationsData {
    const embedded = response._embedded as {
        eventSummaryDtoList?: Array<{
            id?: {value?: string};
            name?: string;
            eventDate?: string;
            location?: string;
            _links?: {self?: {href: string}};
        }>;
    } | undefined;

    const rawItems = embedded?.eventSummaryDtoList ?? [];
    const items: UpcomingRegistrationItem[] = rawItems
        .filter(e => e._links?.self?.href)
        .map(e => ({
            selfHref: toHref(e._links!.self!),
            name: e.name ?? '',
            eventDate: e.eventDate ?? '',
            location: e.location ?? '',
        }));

    return {items};
}

export function useMyUpcomingRegistrations(href: string | undefined) {
    return useAuthorizedQuery(href ?? '', {
        enabled: !!href,
        select: (data) => toUpcomingRegistrationsData(data as HalResponse),
        retry: 1,
    });
}
