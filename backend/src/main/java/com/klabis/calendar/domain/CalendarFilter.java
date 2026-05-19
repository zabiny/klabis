package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemKind;
import com.klabis.events.EventId;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.Set;

/**
 * Repository criterion for querying calendar items.
 * <p>
 * Empty {@code itemTypes} means no restriction on item type — all kinds are included.
 * Empty {@code eventIds} means no restriction on event link — all items are included.
 * <p>
 * Callers that need "items linked to a specific non-empty set of events" should ensure
 * the set is non-empty before calling the repository. When the intent is "no events match",
 * the caller should return {@code List.of()} directly rather than passing an empty set.
 */
@ValueObject
public record CalendarFilter(
        LocalDate startDate,
        LocalDate endDate,
        Set<CalendarItemKind> itemTypes,
        Set<EventId> eventIds
) {

    public CalendarFilter {
        Assert.notNull(startDate, "startDate must not be null");
        Assert.notNull(endDate, "endDate must not be null");
        Assert.isTrue(!endDate.isBefore(startDate), "endDate must not be before startDate");
        itemTypes = itemTypes == null ? Set.of() : Set.copyOf(itemTypes);
        eventIds = eventIds == null ? Set.of() : Set.copyOf(eventIds);
    }

    public static CalendarFilter dateRange(LocalDate startDate, LocalDate endDate) {
        return new CalendarFilter(startDate, endDate, Set.of(), Set.of());
    }

    public CalendarFilter withItemTypes(Set<CalendarItemKind> types) {
        return new CalendarFilter(startDate, endDate, types, eventIds);
    }

    public CalendarFilter withEventIds(Set<EventId> ids) {
        return new CalendarFilter(startDate, endDate, itemTypes, ids);
    }
}
