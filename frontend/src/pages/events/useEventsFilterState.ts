import {useCallback, useEffect, useMemo, useRef} from "react";
import {useSearchParams} from "react-router-dom";
import type {EventsFilterValue} from "../../components/events/EventsFilterBar.tsx";
import {
    combineYearAndTimeWindowToDateParams,
    DEFAULT_TIME_WINDOW,
    getDefaultSortForTimeWindow,
    getTodayIso,
    getTimeWindowFromWhenParam,
    isCurrentYear,
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
    const yearParamPresent = searchParams.has('year');
    const urlWhen = searchParams.get('when');
    const urlQ = searchParams.get('q') ?? '';
    const urlRegisteredByMe = searchParams.get('registeredBy') === REGISTERED_BY_ME;
    const urlEventTypeIds = searchParams.getAll('eventTypeId');

    const currentYear = new Date().getFullYear();
    const parsedUrlYear = urlYear !== null && urlYear !== '' ? parseInt(urlYear, 10) : null;
    const selectedYear: number | null = yearParamPresent
        ? (parsedUrlYear !== null && !Number.isNaN(parsedUrlYear) ? parsedUrlYear : null)
        : currentYear;

    const yearIsNonCurrentAndSet = yearParamPresent && urlYear !== '' && selectedYear !== null && !isCurrentYear(selectedYear);
    const urlWhenNeedsCoercion = urlWhen === 'budouci' || urlWhen === 'probehle';

    // Derive the effective time window synchronously: non-current year with budouci/probehle is
    // meaningless (today falls outside the year), so treat it as vse from the very first render.
    const effectiveTimeWindow: TimeWindow = yearIsNonCurrentAndSet && urlWhenNeedsCoercion
        ? 'vse'
        : getTimeWindowFromWhenParam(urlWhen);

    // If the missing ?when= would default to budouci/probehle for a non-current year, use vse.
    const desiredWhen: TimeWindow = yearIsNonCurrentAndSet
        ? 'vse'
        : (urlWhen ? getTimeWindowFromWhenParam(urlWhen) : DEFAULT_TIME_WINDOW);

    const defaultAppliedRef = useRef(false);
    useEffect(() => {
        if (defaultAppliedRef.current) return;
        defaultAppliedRef.current = true;

        const needsYearDefault = !yearParamPresent;
        const needsWhenWrite = !urlWhen || urlWhen !== desiredWhen;

        if (needsYearDefault || needsWhenWrite) {
            setSearchParams(
                (prev) => {
                    const next = new URLSearchParams(prev);
                    if (needsYearDefault) next.set('year', String(currentYear));
                    if (needsWhenWrite) next.set('when', desiredWhen);
                    return next;
                },
                {replace: true},
            );
        }
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    const filterValue: EventsFilterValue = useMemo(() => ({
        q: urlQ,
        timeWindow: effectiveTimeWindow,
        registeredByMe: urlRegisteredByMe,
        eventTypeIds: urlEventTypeIds,
        selectedYear,
    // urlEventTypeIds is a new array on every render — compare its join to avoid infinite loops
    }), [urlQ, effectiveTimeWindow, urlRegisteredByMe, urlEventTypeIds.join(','), selectedYear]); // eslint-disable-line react-hooks/exhaustive-deps

    const handleFilterChange = useCallback((next: EventsFilterValue) => {
        setSearchParams((prev) => {
            const params = new URLSearchParams(prev);

            if (next.selectedYear !== null && next.selectedYear !== undefined) {
                params.set('year', String(next.selectedYear));
            } else {
                // empty string = sentinel for "no year" (absent param defaults to current year)
                params.set('year', '');
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
    const combinedDateParams = combineYearAndTimeWindowToDateParams(selectedYear, effectiveTimeWindow, today);

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

    const defaultSort = getDefaultSortForTimeWindow(effectiveTimeWindow);

    return {filterValue, extraParams, defaultSort, handleFilterChange};
}
