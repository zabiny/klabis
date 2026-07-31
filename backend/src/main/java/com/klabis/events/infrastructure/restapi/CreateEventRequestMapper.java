package com.klabis.events.infrastructure.restapi;

import com.klabis.events.EventCategory;
import com.klabis.events.EventCategoryId;
import com.klabis.events.domain.Event;

import java.util.List;

class CreateEventRequestMapper {

    private CreateEventRequestMapper() {}

    static Event.CreateEvent toCommand(CreateEventRequest request) {
        return new Event.CreateEvent(
                request.name(),
                request.eventDate(),
                request.location(),
                request.organizer(),
                request.websiteUrl(),
                EventRequestConversions.toCoordinators(request.coordinators()),
                EventRequestConversions.toEventTypeId(request.eventTypeId()),
                EventRequestConversions.toRegistrationDeadlines(request.deadlines()),
                toCategories(request.categories())
        );
    }

    /**
     * Every category on a brand-new event is new, so the server assigns a fresh id — the create
     * schema has no {@code id} property at all, unlike the update one.
     */
    private static List<EventCategory> toCategories(List<CreateEventCategoryRequest> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream()
                .map(category -> new EventCategory(
                        EventCategoryId.generate(),
                        null,
                        category.name(),
                        EventRequestConversions.toMoney(category.fee())))
                .toList();
    }
}
