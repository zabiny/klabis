package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.Event;

class EventDtoMapper {

    static EventDto toDto(Event event) {
        return new EventDto(
                event.getId().value(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null,
                event.getEventCoordinatorId() != null ? event.getEventCoordinatorId().uuid() : null,
                event.getStatus()
        );
    }

    static EventSummaryDto toSummaryDto(Event event) {
        return new EventSummaryDto(
                event.getId().value(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getStatus()
        );
    }

    private EventDtoMapper() {
    }
}
