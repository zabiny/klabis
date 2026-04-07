package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.Event;

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
                event.getRegistrationDeadline(),
                event.getStatus(),
                event.getCategories()
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
                event.getRegistrationDeadline(),
                event.getStatus(),
                event.getCategories()
        );
    }

    private EventDtoMapper() {
    }
}
