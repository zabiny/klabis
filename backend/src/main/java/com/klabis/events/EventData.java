package com.klabis.events;

import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.LocalDate;
import java.util.Set;

/**
 * Read-only snapshot of event data consumed by other modules (calendar synchronization and iCal feed).
 *
 * @param id                   event identifier
 * @param name                 event name
 * @param eventDate            event date
 * @param location             event location
 * @param organizer            event organizer
 * @param websiteUrl           event website URL (may be null)
 * @param registrationDeadline registration deadline (may be null when not set)
 * @param cancelled            whether the event is cancelled
 * @param coordinators         members coordinating the event
 */
@RecordBuilder
public record EventData(
        EventId id,
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        String websiteUrl,
        LocalDate registrationDeadline,
        boolean cancelled,
        Set<MemberId> coordinators
) {

    public boolean isCoordinator(MemberId memberId) {
        return coordinators.contains(memberId);
    }
}
