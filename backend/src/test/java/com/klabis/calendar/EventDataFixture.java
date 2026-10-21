package com.klabis.calendar;

import com.klabis.events.EventData;
import com.klabis.events.EventDataBuilder;

import java.time.LocalDate;
import java.util.Set;

public final class EventDataFixture {

    private EventDataFixture() {
    }

    public static EventData eventData(String name, LocalDate eventDate, String location, String organizer,
                                      String websiteUrl, LocalDate registrationDeadline) {
        return EventDataBuilder.builder()
                .name(name)
                .eventDate(eventDate)
                .location(location)
                .organizer(organizer)
                .websiteUrl(websiteUrl)
                .registrationDeadline(registrationDeadline)
                .cancelled(false)
                .coordinators(Set.of())
                .build();
    }
}
