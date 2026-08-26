package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.EventType;

class EventTypeDtoMapper {

    private EventTypeDtoMapper() {
    }

    static EventTypeDto toDto(EventType eventType) {
        return EventTypeDtoBuilder.builder()
                .id(eventType.getId().value())
                .name(eventType.getName())
                .color(eventType.getColor().orElse(null))
                .sortOrder(eventType.getSortOrder())
                .orisDisciplineIds(eventType.getOrisDisciplineIds())
                .build();
    }
}
