import type {HalResponse} from '../api';
import type {HalFormsTemplate} from '../api';
import {toHref} from '../api/hateoas';
import {useAuthorizedQuery} from './useAuthorizedFetch';

export interface UpcomingDeadlineItem {
    selfHref: string;
    name: string;
    eventDate: string;
    deadline: string;
    newRegistrationHref: string | undefined;
    registerForEventTemplate: HalFormsTemplate | undefined;
}

export interface UpcomingDeadlinesData {
    items: UpcomingDeadlineItem[];
    totalElements: number;
}

function pickNextRelevantDeadline(deadlines: string[] | undefined): string {
    if (!deadlines || deadlines.length === 0) return '';
    const today = new Date().toISOString().slice(0, 10);
    return deadlines.find(d => d >= today) ?? deadlines[deadlines.length - 1];
}

function toUpcomingDeadlinesData(response: HalResponse): UpcomingDeadlinesData {
    const embedded = response._embedded as {
        eventSummaryDtoList?: Array<{
            id?: {value?: string};
            name?: string;
            eventDate?: string;
            deadlines?: string[];
            _links?: {
                self?: {href: string};
                newRegistration?: {href: string};
            };
            _templates?: Record<string, HalFormsTemplate>;
        }>;
    } | undefined;

    const page = (response as {page?: {totalElements?: number}}).page;
    const totalElements = page?.totalElements ?? 0;

    const rawItems = embedded?.eventSummaryDtoList ?? [];
    const items: UpcomingDeadlineItem[] = rawItems
        .filter(e => e._links?.self?.href)
        .map(e => ({
            selfHref: toHref(e._links!.self!),
            name: e.name ?? '',
            eventDate: e.eventDate ?? '',
            deadline: pickNextRelevantDeadline(e.deadlines),
            newRegistrationHref: e._links?.newRegistration ? toHref(e._links.newRegistration) : undefined,
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
