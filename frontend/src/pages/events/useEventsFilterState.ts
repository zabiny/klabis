import {useCallback, useEffect, useMemo, useRef} from "react";
import {useSearchParams} from "react-router-dom";
import type {EventsFilterValue} from "../../components/events/EventsFilterBar.tsx";
import {
    DEFAULT_TIME_WINDOW,
    getDefaultSortForTimeWindow,
    getTimeWindowFromParams,
    getTodayIso,
    getYearFromParams,
    REGISTERED_BY_ME,
    type TimeWindow,
    timeWindowToDateParams,
    yearToDateParams,
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

    const urlDateFrom = searchParams.get('dateFrom');
    const urlDateTo = searchParams.get('dateTo');
    const urlQ = searchParams.get('q') ?? '';
    const urlRegisteredByMe = searchParams.get('registeredBy') === REGISTERED_BY_ME;
    const urlEventTypeIds = searchParams.getAll('eventTypeId');

    const selectedYear = getYearFromParams(urlDateFrom, urlDateTo);
    const timeWindow: TimeWindow = selectedYear !== null ? 'vse' : getTimeWindowFromParams(urlDateFrom, urlDateTo);

    const defaultAppliedRef = useRef(false);
    useEffect(() => {
        if (defaultAppliedRef.current) return;
        defaultAppliedRef.current = true;

        if (!urlDateFrom && !urlDateTo) {
            const today = getTodayIso();
            const {dateFrom} = timeWindowToDateParams(DEFAULT_TIME_WINDOW, today);
            setSearchParams(
                (prev) => {
                    const next = new URLSearchParams(prev);
                    if (dateFrom) next.set('dateFrom', dateFrom);
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
        const today = getTodayIso();
        setSearchParams((prev) => {
            const params = new URLSearchParams(prev);
            if (next.selectedYear !== null && next.selectedYear !== undefined) {
                const {dateFrom, dateTo} = yearToDateParams(next.selectedYear);
                params.set('dateFrom', dateFrom);
                params.set('dateTo', dateTo);
            } else {
                const {dateFrom, dateTo} = timeWindowToDateParams(next.timeWindow, today);
                if (dateFrom) { params.set('dateFrom', dateFrom); } else { params.delete('dateFrom'); }
                if (dateTo) { params.set('dateTo', dateTo); } else { params.delete('dateTo'); }
            }
            if (next.q) { params.set('q', next.q); } else { params.delete('q'); }
            if (next.registeredByMe) { params.set('registeredBy', REGISTERED_BY_ME); } else { params.delete('registeredBy'); }
            params.delete('eventTypeId');
            next.eventTypeIds.forEach((id) => params.append('eventTypeId', id));
            return params;
        });
    }, [setSearchParams]);

    const extraParams = useMemo((): Record<string, string | string[]> => {
        const params: Record<string, string | string[]> = {};
        if (urlDateFrom) params.dateFrom = urlDateFrom;
        if (urlDateTo) params.dateTo = urlDateTo;
        if (urlQ && urlQ.length >= 2) params.q = urlQ;
        if (urlRegisteredByMe) params.registeredBy = REGISTERED_BY_ME;
        if (urlEventTypeIds.length > 0) params.eventTypeId = urlEventTypeIds;
        return params;
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [urlDateFrom, urlDateTo, urlQ, urlRegisteredByMe, urlEventTypeIds.join(',')]);

    const defaultSort = getDefaultSortForTimeWindow(timeWindow);

    return {filterValue, extraParams, defaultSort, handleFilterChange};
}
