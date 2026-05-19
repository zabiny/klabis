package com.klabis.events;

import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.time.LocalDate;
import java.util.Set;

/**
 * Cross-module query port for resolving event IDs relevant to a member's personal schedule.
 * <p>
 * Used by the calendar module to determine which events a given member is involved in
 * (as a registered participant or as the coordinator) within a given date range.
 * Results from the two query methods are unioned in the consuming use case to express
 * OR semantics — which cannot be expressed via a single {@link com.klabis.events.domain.EventFilter}
 * without introducing a disjunctive field.
 */
@SecondaryPort
public interface EventScheduleQuery {

    /**
     * Returns the IDs of events within the given date range for which the given member
     * has an active registration.
     */
    Set<EventId> findEventIdsByRegistration(MemberId memberId, LocalDate from, LocalDate to);

    /**
     * Returns the IDs of events within the given date range for which the given member
     * is the coordinator.
     */
    Set<EventId> findEventIdsByCoordinator(MemberId memberId, LocalDate from, LocalDate to);
}
