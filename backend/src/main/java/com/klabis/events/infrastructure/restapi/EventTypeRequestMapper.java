package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.EventType;

class EventTypeRequestMapper {

    private EventTypeRequestMapper() {
    }

    static EventType.CreateEventType toCommand(CreateEventTypeRequest request) {
        return new EventType.CreateEventType(
                request.name(),
                request.color(),
                request.sortOrder(),
                request.orisDisciplineIds()
        );
    }

    static EventType.UpdateEventType toCommand(UpdateEventTypeRequest request) {
        return new EventType.UpdateEventType(
                request.name(),
                request.color(),
                request.sortOrder(),
                request.orisDisciplineIds()
        );
    }
}
