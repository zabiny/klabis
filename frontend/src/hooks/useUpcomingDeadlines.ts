import type {Link} from '../api';
import {asLinkArray, selfHref} from '../api/hateoas';
import {useHalCollection} from './useHalCollection';
import type {EventSummaryRow} from './eventSummaryRow';
import {getTodayIso} from '../utils/dateUtils';

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
    events.flatMap(e => {
        const href = selfHref(e);
        if (href === undefined) return [];
        return [{
            selfHref: href,
            name: e.name ?? '',
            eventDate: e.eventDate ?? '',
            deadline: pickNextRelevantDeadline(e.deadlines),
            newRegistration: asLinkArray(e._links?.newRegistration)[0],
        }];
    });

export function useUpcomingDeadlines(href: string | undefined): UseUpcomingDeadlinesResult {
    const {items, pageMetadata, refetch} = useHalCollection<EventSummaryRow, UpcomingDeadlineItem>(href, {
        select: toUpcomingDeadlineItems,
    });

    return {items, totalElements: pageMetadata?.totalElements ?? 0, refetch};
}
