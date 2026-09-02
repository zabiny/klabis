import type {HalResponse, Link} from '../api';
import {isLink, toHref} from '../api/hateoas';
import type {HalResourceLinks} from '../api';
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

function newRegistrationLink(event: components['schemas']['EntityModelEventSummaryDto']): Link | undefined {
    const link = firstLink(event._links?.newRegistration);
    return isLink(link) ? link : undefined;
}

export interface UpcomingDeadlineItem {
    selfHref: string;
    name: string;
    eventDate: string;
    deadline: string;
    newRegistration: Link | undefined;
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
        eventSummaryDtoList?: Array<components['schemas']['EntityModelEventSummaryDto']>;
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
            deadline: pickNextRelevantDeadline(e.deadlines),
            newRegistration: newRegistrationLink(e),
        }));

    return {items, totalElements};
}

export function useUpcomingDeadlines(href: string | undefined) {
    return useAuthorizedQuery(href ?? '', {
        enabled: !!href,
        select: (data) => toUpcomingDeadlinesData(data as HalResponse),
    });
}
