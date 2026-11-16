import type {HalResourceLinks, Link} from '../api';
import {asLinkArray, toHref} from '../api/hateoas';
import type {components} from '../api/klabisApi';
import {useHalCollection} from './useHalCollection';
import {getTodayIso} from '../utils/dateUtils';

type EventSummaryRow = components['schemas']['EntityModelEventSummaryDto'];

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

export interface UseUpcomingDeadlinesResult {
    items: UpcomingDeadlineItem[];
    totalElements: number;
    refetch: () => void;
}

function pickNextRelevantDeadline(deadlines: string[] | undefined): string {
    if (!deadlines || deadlines.length === 0) return '';
    const today = getTodayIso();
    return deadlines.find(d => d >= today) ?? deadlines[deadlines.length - 1];
}

const toUpcomingDeadlineItems = (events: EventSummaryRow[]): UpcomingDeadlineItem[] =>
    events
        .filter(e => asLinkArray(e._links?.self)[0])
        .map(e => ({
            selfHref: linkHref(e._links?.self)!,
            name: e.name ?? '',
            eventDate: e.eventDate ?? '',
            deadline: pickNextRelevantDeadline(e.deadlines),
            newRegistration: asLinkArray(e._links?.newRegistration)[0],
        }));

export function useUpcomingDeadlines(href: string | undefined): UseUpcomingDeadlinesResult {
    const {items, pageMetadata, refetch} = useHalCollection<EventSummaryRow, UpcomingDeadlineItem>(href, {
        select: toUpcomingDeadlineItems,
    });

    return {items, totalElements: pageMetadata?.totalElements ?? 0, refetch};
}
