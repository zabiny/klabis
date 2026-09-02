import type {HalResourceLinks, HalResponse, Link} from '../api';
import {asLinkArray, toHref} from '../api/hateoas';
import type {components} from '../api/klabisApi';
import {useAuthorizedQuery} from './useAuthorizedFetch';
import {getTodayIso} from '../utils/dateUtils';

function linkHref(links: HalResourceLinks | undefined): string | undefined {
    const link = asLinkArray(links)[0];
    return link ? toHref(link) : undefined;
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
        .filter(e => asLinkArray(e._links?.self)[0])
        .map(e => ({
            selfHref: linkHref(e._links?.self)!,
            name: e.name ?? '',
            eventDate: e.eventDate ?? '',
            deadline: pickNextRelevantDeadline(e.deadlines),
            newRegistration: asLinkArray(e._links?.newRegistration)[0],
        }));

    return {items, totalElements};
}

export function useUpcomingDeadlines(href: string | undefined) {
    return useAuthorizedQuery(href ?? '', {
        enabled: !!href,
        select: (data) => toUpcomingDeadlinesData(data as HalResponse),
    });
}
