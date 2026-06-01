package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.EventType;

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
