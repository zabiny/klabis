package com.klabis.events.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Encapsulates query criteria for filtering events.
 * <p>
 * All fields are optional — null or empty means "no filter on this dimension".
 * Factory methods cover the most common filtering scenarios.
 */
@ValueObject
public record EventFilter(
        Set<EventStatus> statuses,
        String organizer,
        LocalDate dateFrom,
        LocalDate dateTo
) {

    public EventFilter {
        statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
    }

    /**
     * No filtering — returns all events.
     */
    public static EventFilter none() {
        return new EventFilter(Set.of(), null, null, null);
    }

    /**
     * Filter to events whose status is one of the given statuses.
     */
    public static EventFilter byStatus(EventStatus... statuses) {
        return new EventFilter(Set.copyOf(Arrays.asList(statuses)), null, null, null);
    }

    /**
     * Filter to events whose status is NOT any of the given statuses.
     * Uses {@link EnumSet#complementOf} to compute the allowed set.
     */
    public static EventFilter byNotHavingStatus(EventStatus... excluded) {
        EnumSet<EventStatus> excludedSet = EnumSet.copyOf(Arrays.asList(excluded));
        EnumSet<EventStatus> allowed = EnumSet.complementOf(excludedSet);
        return new EventFilter(allowed, null, null, null);
    }

    public static EventFilter byOrganizer(String organizer) {
        return new EventFilter(Set.of(), organizer, null, null);
    }

    public static EventFilter byDateRange(LocalDate from, LocalDate to) {
        return new EventFilter(Set.of(), null, from, to);
    }
}
