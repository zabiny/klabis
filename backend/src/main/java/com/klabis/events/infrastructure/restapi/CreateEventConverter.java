package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.events.EventCategory;
import com.klabis.events.EventCategoryId;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.events.domain.SiPolicy;
import com.klabis.events.domain.SiUsage;
import com.klabis.members.MemberId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * MapStruct mapper for {@link CreateEventRequest} to {@link Event.CreateEvent}, wired as a Spring
 * {@code Converter} via {@code MapstructSpringMapperConfig}. Wire-to-domain steps shared with the
 * PATCH path stay in {@link EventRequestConversions}; the default methods only delegate to them
 * (never {@code uses =} on a Converter — see backend-patterns).
 * <p>
 * Absent coordinators become an empty set rather than null: the command's field is a plain
 * {@code LinkedHashSet}, and the domain reads "no coordinators" from emptiness.
 */
@Mapper(config = MapstructSpringMapperConfig.class,
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
interface CreateEventConverter extends Converter<CreateEventRequest, Event.CreateEvent> {

    @Override
    @Mapping(target = "registrationDeadlines", source = "deadlines")
    @Mapping(target = "siPolicy", source = ".")
    Event.CreateEvent convert(CreateEventRequest request);

    default MemberId toMemberId(UUID id) {
        return id == null ? null : new MemberId(id);
    }

    default EventTypeId toEventTypeId(UUID id) {
        return EventRequestConversions.toEventTypeId(id);
    }

    default RegistrationDeadlines toRegistrationDeadlines(List<LocalDate> deadlines) {
        return EventRequestConversions.toRegistrationDeadlines(deadlines);
    }

    SiUsage toSiUsage(CreateEventRequestSiUsage usage);

    default SiPolicy toSiPolicy(CreateEventRequest request) {
        return SiPolicy.of(toSiUsage(request.siUsage()), Boolean.TRUE.equals(request.siRentalAllowed()));
    }

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
                        toMoney(category.fee())))
                .toList();
    }

    default Money toMoney(EntryFeeRequest fee) {
        return EventRequestConversions.toMoney(fee);
    }
}
