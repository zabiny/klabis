package com.klabis.events;

import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.time.LocalDate;
import java.util.Set;

/**
 * Cross-module query port for resolving event IDs relevant to a member's personal schedule.
 * <p>
 * Used by the calendar module to determine which events a given member is involved in
 * (as an active participant or the coordinator) within a given date range.
 */
@SecondaryPort
public interface EventScheduleQuery {

    /**
     * Returns the IDs of events whose date falls within {@code [from, to]} (both inclusive)
     * and where the given member is either the coordinator or has an active registration.
     */
    Set<EventId> findEventIdsForMemberSchedule(MemberId memberId, LocalDate from, LocalDate to);
}
