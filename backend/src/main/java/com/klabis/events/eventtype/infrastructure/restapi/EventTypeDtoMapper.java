package com.klabis.events.eventtype.infrastructure.restapi;

import com.klabis.events.eventtype.domain.EventType;

class EventTypeDtoMapper {

    private EventTypeDtoMapper() {
    }

    static EventTypeDto toDto(EventType eventType) {
        return new EventTypeDto(
                eventType.getId(),
                eventType.getName(),
                eventType.getColor().orElse(null),
                eventType.getSortOrder(),
                eventType.getOrisDisciplineIds()
        );
    }
}
