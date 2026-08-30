package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.events.EventCategory;
import com.klabis.events.EventCategoryId;
import com.klabis.events.domain.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

/**
 * MapStruct mapper for {@link CreateEventRequest} to {@link Event.CreateEvent}, wired as a Spring
 * {@code Converter} via {@code MapstructSpringMapperConfig}. Wire-to-domain steps shared with the
 * PATCH path stay in {@link EventRequestConversions}; this interface only calls into them (never
 * {@code uses =} on a Converter — see backend-patterns).
 */
@Mapper(config = MapstructSpringMapperConfig.class)
interface CreateEventConverter extends Converter<CreateEventRequest, Event.CreateEvent> {

    @Override
    @Mapping(target = "coordinators", expression = "java(EventRequestConversions.toCoordinators(request.coordinators()))")
    @Mapping(target = "eventTypeId", expression = "java(EventRequestConversions.toEventTypeId(request.eventTypeId()))")
    @Mapping(target = "registrationDeadlines", expression = "java(EventRequestConversions.toRegistrationDeadlines(request.deadlines()))")
    @Mapping(target = "categories", source = "categories")
    Event.CreateEvent convert(CreateEventRequest request);

    /**
     * Every category on a brand-new event is new, so the server assigns a fresh id — the create
     * schema has no {@code id} property at all, unlike the update one.
     */
    default List<EventCategory> toCategories(List<CreateEventCategoryRequest> categories) {
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
