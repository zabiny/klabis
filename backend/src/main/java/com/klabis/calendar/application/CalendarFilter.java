package com.klabis.calendar.application;

import com.klabis.members.MemberId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

import java.time.LocalDate;

/**
 * Port-layer input for listing calendar items.
 * Bundles the date range, sort, and optional mySchedule filter into a single value.
 *
 * When {@code myScheduleMemberId} is non-null, the personal-schedule filter is active.
 * When it is null and {@code myScheduleRequested} is true, the caller has no member profile —
 * the service short-circuits to an empty result without hitting the events module.
 */
public record CalendarFilter(
        LocalDate startDate,
        LocalDate endDate,
        Sort sort,
        boolean myScheduleRequested,
        @Nullable MemberId myScheduleMemberId
) {
    public CalendarFilter {
        Assert.notNull(startDate, "startDate must not be null");
        Assert.notNull(endDate, "endDate must not be null");
        Assert.notNull(sort, "sort must not be null");
    }
}
