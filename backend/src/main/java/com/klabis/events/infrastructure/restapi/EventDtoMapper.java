package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.Event;
import com.klabis.events.domain.RegistrationDeadlines;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

class EventDtoMapper {

    static EventDto toDto(Event event) {
        return new EventDto(
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null,
                event.getEventCoordinatorId(),
                event.getStatus(),
                event.getCategories(),
                event.getCancellationReason().orElse(null),
                toDeadlineList(event.getRegistrationDeadlines())
        );
    }

    static EventSummaryDto toSummaryDto(Event event) {
        return new EventSummaryDto(
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null,
                event.getStatus(),
                event.getCategories(),
                event.getCancellationReason().orElse(null),
                toDeadlineList(event.getRegistrationDeadlines())
        );
    }

    private static List<LocalDate> toDeadlineList(RegistrationDeadlines deadlines) {
        if (deadlines == null || deadlines.isEmpty()) {
            return null;
        }
        return Stream.of(
                        deadlines.deadline1().orElse(null),
                        deadlines.deadline2().orElse(null),
                        deadlines.deadline3().orElse(null))
                .filter(d -> d != null)
                .toList();
    }

    private EventDtoMapper() {
    }
}
