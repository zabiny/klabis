import {useCallback, useEffect, useMemo, useRef} from "react";
import {useSearchParams} from "react-router-dom";
import type {EventsFilterValue} from "../../components/events/EventsFilterBar.tsx";
import {
    combineYearAndTimeWindowToDateParams,
    DEFAULT_TIME_WINDOW,
    getDefaultSortForTimeWindow,
    getTodayIso,
    getTimeWindowFromWhenParam,
    REGISTERED_BY_ME,
    type TimeWindow,
} from "../../components/events/eventsFilterUtils.ts";

interface DefaultSort {
    by: string;
    direction: 'asc' | 'desc';
}

interface EventsFilterState {
    filterValue: EventsFilterValue;
    extraParams: Record<string, string | string[]>;
    defaultSort: DefaultSort;
    handleFilterChange: (next: EventsFilterValue) => void;
}

export function useEventsFilterState(): EventsFilterState {
    const [searchParams, setSearchParams] = useSearchParams();

    const urlYear = searchParams.get('year');
    const urlWhen = searchParams.get('when');
    const urlQ = searchParams.get('q') ?? '';
    const urlRegisteredByMe = searchParams.get('registeredBy') === REGISTERED_BY_ME;
    const urlEventTypeIds = searchParams.getAll('eventTypeId');

    const selectedYear: number | null = urlYear !== null ? parseInt(urlYear, 10) : null;
    const timeWindow: TimeWindow = getTimeWindowFromWhenParam(urlWhen);

    const defaultAppliedRef = useRef(false);
    useEffect(() => {
        if (defaultAppliedRef.current) return;
        defaultAppliedRef.current = true;

        if (!urlWhen) {
            setSearchParams(
                (prev) => {
                    const next = new URLSearchParams(prev);
                    next.set('when', DEFAULT_TIME_WINDOW);
                    return next;
                },
                {replace: true},
            );
        }
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    const filterValue: EventsFilterValue = useMemo(() => ({
        q: urlQ,
        timeWindow,
        registeredByMe: urlRegisteredByMe,
        eventTypeIds: urlEventTypeIds,
        selectedYear,
    // urlEventTypeIds is a new array on every render — compare its join to avoid infinite loops
    }), [urlQ, timeWindow, urlRegisteredByMe, urlEventTypeIds.join(','), selectedYear]); // eslint-disable-line react-hooks/exhaustive-deps

    const handleFilterChange = useCallback((next: EventsFilterValue) => {
        setSearchParams((prev) => {
            const params = new URLSearchParams(prev);

            if (next.selectedYear !== null && next.selectedYear !== undefined) {
                params.set('year', String(next.selectedYear));
            } else {
                params.delete('year');
            }

            params.set('when', next.timeWindow);

            // Remove legacy date params to avoid confusion
            params.delete('dateFrom');
            params.delete('dateTo');

            if (next.q) { params.set('q', next.q); } else { params.delete('q'); }
            if (next.registeredByMe) { params.set('registeredBy', REGISTERED_BY_ME); } else { params.delete('registeredBy'); }
            params.delete('eventTypeId');
            next.eventTypeIds.forEach((id) => params.append('eventTypeId', id));
            return params;
        });
    }, [setSearchParams]);

    const today = getTodayIso();
    const combinedDateParams = combineYearAndTimeWindowToDateParams(selectedYear, timeWindow, today);

    const extraParams = useMemo((): Record<string, string | string[]> => {
        const params: Record<string, string | string[]> = {};
        if (combinedDateParams.dateFrom) params.dateFrom = combinedDateParams.dateFrom;
        if (combinedDateParams.dateTo) params.dateTo = combinedDateParams.dateTo;
        if (urlQ && urlQ.length >= 2) params.q = urlQ;
        if (urlRegisteredByMe) params.registeredBy = REGISTERED_BY_ME;
        if (urlEventTypeIds.length > 0) params.eventTypeId = urlEventTypeIds;
        return params;
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [combinedDateParams.dateFrom, combinedDateParams.dateTo, urlQ, urlRegisteredByMe, urlEventTypeIds.join(',')]);

    const defaultSort = getDefaultSortForTimeWindow(timeWindow);

    return {filterValue, extraParams, defaultSort, handleFilterChange};
}
