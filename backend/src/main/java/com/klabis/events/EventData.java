package com.klabis.events;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.LocalDate;

/**
 * Data Transfer Object for event data needed by calendar event handlers.
 * <p>
 * This record encapsulates the event properties required to create and update
 * calendar items during event-driven synchronization.
 *
 * @param name        event name
 * @param eventDate   event date
 * @param location    event location
 * @param organizer   event organizer
 * @param websiteUrl  event website URL (may be null)
 */
@RecordBuilder
public record EventData(
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        String websiteUrl
) {
}
