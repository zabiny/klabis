package com.klabis.events.domain;

import com.klabis.members.MemberId;
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
        LocalDate dateTo,
        String fulltextQuery,
        MemberId registeredBy,
        MemberId coordinator
) {

    public EventFilter {
        statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
        if (fulltextQuery != null) {
            fulltextQuery = fulltextQuery.trim().isEmpty() ? null : fulltextQuery.trim();
        }
    }

    /**
     * No filtering — returns all events.
     */
    public static EventFilter none() {
        return new EventFilter(Set.of(), null, null, null, null, null, null);
    }

    /**
     * Filter to events whose status is one of the given statuses.
     */
    public static EventFilter byStatus(EventStatus... statuses) {
        return new EventFilter(Set.copyOf(Arrays.asList(statuses)), null, null, null, null, null, null);
    }

    /**
     * Filter to events whose status is NOT any of the given statuses.
     * Uses {@link EnumSet#complementOf} to compute the allowed set.
     */
    public static EventFilter byNotHavingStatus(EventStatus... excluded) {
        EnumSet<EventStatus> excludedSet = EnumSet.copyOf(Arrays.asList(excluded));
        EnumSet<EventStatus> allowed = EnumSet.complementOf(excludedSet);
        return new EventFilter(allowed, null, null, null, null, null, null);
    }

    /**
     * Returns true when the filter's status set explicitly contains this status
     * and no others — i.e., caller is asking for only this status.
     */
    public boolean requestsOnlyStatus(EventStatus status) {
        return statuses.size() == 1 && statuses.contains(status);
    }

    /**
     * Returns true when the filter already guarantees this status cannot appear in results —
     * i.e., the filter has an explicit include-set that does not contain this status.
     * A none-filter (empty set) returns false because it imposes no restriction yet.
     */
    public boolean excludesStatus(EventStatus status) {
        return !statuses.isEmpty() && !statuses.contains(status);
    }

    /**
     * Returns a new filter identical to this one but with the given status removed
     * from the allowed set.  When the filter had no status restriction (empty set),
     * the complement of the excluded status is used instead.
     */
    public EventFilter withExcludedStatus(EventStatus excluded) {
        if (statuses.isEmpty()) {
            return EventFilter.byNotHavingStatus(excluded);
        }
        EnumSet<EventStatus> remaining = EnumSet.copyOf(statuses);
        remaining.remove(excluded);
        if (remaining.isEmpty()) {
            return new EventFilter(Set.of(), organizer, dateFrom, dateTo, fulltextQuery, registeredBy, coordinator);
        }
        return new EventFilter(remaining, organizer, dateFrom, dateTo, fulltextQuery, registeredBy, coordinator);
    }

    /**
     * Returns a new filter identical to this one but with the given fulltext query applied.
     * Leading/trailing whitespace is trimmed; blank input clears the query (no filtering).
     */
    public EventFilter withFulltext(String query) {
        return new EventFilter(statuses, organizer, dateFrom, dateTo, query, registeredBy, coordinator);
    }

    /**
     * Returns a new filter identical to this one but restricted to events from the given organizer.
     * Null clears the restriction.
     */
    public EventFilter withOrganizer(String organizerCode) {
        return new EventFilter(statuses, organizerCode, dateFrom, dateTo, fulltextQuery, registeredBy, coordinator);
    }

    /**
     * Returns a new filter identical to this one but restricted to events where the given
     * member has a registration. Null clears the restriction.
     */
    public EventFilter withRegisteredBy(MemberId memberId) {
        return new EventFilter(statuses, organizer, dateFrom, dateTo, fulltextQuery, memberId, coordinator);
    }

    /**
     * Returns a new filter identical to this one but restricted to events where the given
     * member is the coordinator. Null clears the restriction.
     */
    public EventFilter withCoordinator(MemberId memberId) {
        return new EventFilter(statuses, organizer, dateFrom, dateTo, fulltextQuery, registeredBy, memberId);
    }

    /**
     * Filter for ACTIVE events whose event date is strictly before {@code date}.
     * Equivalent to: {@code status = 'ACTIVE' AND event_date < date}.
     * The inclusive {@code dateTo} is set to {@code date.minusDays(1)} to preserve
     * exclusive-upper-bound semantics that the original SQL query used.
     */
    public static EventFilter activeEventsWithDateBefore(LocalDate date) {
        return new EventFilter(Set.of(EventStatus.ACTIVE), null, null, date.minusDays(1), null, null, null);
    }

    public static EventFilter byOrganizer(String organizer) {
        return new EventFilter(Set.of(), organizer, null, null, null, null, null);
    }

    public static EventFilter byDateRange(LocalDate from, LocalDate to) {
        return new EventFilter(Set.of(), null, from, to, null, null, null);
    }
}
