import {useCallback, useMemo} from "react";
import {useUrlFilters} from "../../hooks/useUrlFilters.ts";
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
    const currentYear = new Date().getFullYear();
    const {values, setValues} = useUrlFilters({
        year: {type: 'string', default: String(currentYear)},
        when: {type: 'string', default: DEFAULT_TIME_WINDOW},
        q: {type: 'string'},
        registeredByMe: {type: 'boolean', param: 'registeredBy', trueValue: REGISTERED_BY_ME},
        eventTypeIds: {type: 'string[]', param: 'eventTypeId'},
    });

    const parsedYear = values.year === '' ? NaN : parseInt(values.year, 10);
    const selectedYear: number | null = Number.isNaN(parsedYear) ? null : parsedYear;

    // today falls outside a non-current year, so budouci/probehle would be meaningless there
    const requestedTimeWindow = getTimeWindowFromWhenParam(values.when);
    const effectiveTimeWindow: TimeWindow =
        selectedYear !== null && !isCurrentYear(selectedYear) && requestedTimeWindow !== 'vse'
            ? 'vse'
            : requestedTimeWindow;

    const {q, registeredByMe, eventTypeIds} = values;

    const filterValue: EventsFilterValue = useMemo(() => ({
        q,
        timeWindow: effectiveTimeWindow,
        registeredByMe,
        eventTypeIds,
        selectedYear,
    }), [q, effectiveTimeWindow, registeredByMe, eventTypeIds, selectedYear]);

    const handleFilterChange = useCallback((next: EventsFilterValue) => {
        setValues({
            year: next.selectedYear === null || next.selectedYear === undefined ? '' : String(next.selectedYear),
            when: next.timeWindow,
            q: next.q,
            registeredByMe: next.registeredByMe,
            eventTypeIds: next.eventTypeIds,
        });
    }, [setValues]);

    const today = getTodayIso();
    const {dateFrom, dateTo} = combineYearAndTimeWindowToDateParams(selectedYear, effectiveTimeWindow, today);

    const extraParams = useMemo((): Record<string, string | string[]> => {
        const params: Record<string, string | string[]> = {};
        if (dateFrom) params.dateFrom = dateFrom;
        if (dateTo) params.dateTo = dateTo;
        if (q.length >= 2) params.q = q;
        if (registeredByMe) params.registeredBy = REGISTERED_BY_ME;
        if (eventTypeIds.length > 0) params.eventTypeId = eventTypeIds;
        return params;
    }, [dateFrom, dateTo, q, registeredByMe, eventTypeIds]);

    const defaultSort = getDefaultSortForTimeWindow(effectiveTimeWindow);

    return {filterValue, extraParams, defaultSort, handleFilterChange};
}
