import type {HalResponse} from '../api';
import type {HalFormsTemplate, HalResourceLinks, Link} from '../api';
import {isLink, toHref} from '../api/hateoas';
import type {components} from '../api/klabisApi';
import {useAuthorizedQuery} from './useAuthorizedFetch';
import {getTodayIso} from '../utils/dateUtils';

function firstLink(links: HalResourceLinks | undefined): Link | undefined {
    return Array.isArray(links) ? links[0] : links;
}

function linkHref(links: HalResourceLinks | undefined): string | undefined {
    const link = firstLink(links);
    return isLink(link) ? toHref(link) : undefined;
}

export interface UpcomingDeadlineItem {
    selfHref: string;
    name: string;
    eventDate: string;
    location: string | undefined;
    deadlines: string[];
    deadline: string;
    sharedTransportEnabled: boolean | undefined;
    sharedAccommodationEnabled: boolean | undefined;
    newRegistrationHref: string | undefined;
    registerForEventTemplate: HalFormsTemplate | undefined;
}

export interface UpcomingDeadlinesData {
    items: UpcomingDeadlineItem[];
    totalElements: number;
}

function pickNextRelevantDeadline(deadlines: string[] | undefined): string {
    if (!deadlines || deadlines.length === 0) return '';
    const today = getTodayIso();
    return deadlines.find(d => d >= today) ?? deadlines[deadlines.length - 1];
}

function toUpcomingDeadlinesData(response: HalResponse): UpcomingDeadlinesData {
    const embedded = response._embedded as {
        eventSummaryDtoList?: Array<components['schemas']['EntityModelEventSummaryDto'] & {
            _templates?: Record<string, HalFormsTemplate>;
        }>;
    } | undefined;

    const page = (response as {page?: {totalElements?: number}}).page;
    const totalElements = page?.totalElements ?? 0;

    const rawItems = embedded?.eventSummaryDtoList ?? [];
    const items: UpcomingDeadlineItem[] = rawItems
        .filter(e => isLink(firstLink(e._links?.self)))
        .map(e => ({
            selfHref: linkHref(e._links?.self)!,
            name: e.name ?? '',
            eventDate: e.eventDate ?? '',
            location: e.location,
            deadlines: e.deadlines ?? [],
            deadline: pickNextRelevantDeadline(e.deadlines),
            sharedTransportEnabled: e.sharedTransportEnabled,
            sharedAccommodationEnabled: e.sharedAccommodationEnabled,
            newRegistrationHref: linkHref(e._links?.newRegistration),
            registerForEventTemplate: e._templates?.registerForEvent,
        }));

    return {items, totalElements};
}

export function useUpcomingDeadlines(href: string | undefined) {
    return useAuthorizedQuery(href ?? '', {
        enabled: !!href,
        select: (data) => toUpcomingDeadlinesData(data as HalResponse),
    });
}
