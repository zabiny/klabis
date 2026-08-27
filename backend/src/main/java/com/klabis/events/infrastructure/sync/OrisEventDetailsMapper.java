package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.dto.EventDetails;
import com.klabis.common.mapping.MapstructSpringMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

/**
 * MapStruct mapper for the trivial field-to-field parts of an ORIS {@link EventDetails}: the plain
 * scalar fields that need no business logic. Everything non-trivial (deadlines, organizer fallback,
 * category merge, max entry fee, event-type lookup) lives in {@link OrisEventMappingSupport}.
 */
@Mapper(config = MapstructSpringMapperConfig.class)
interface OrisEventDetailsMapper {

    @Mapping(target = "name", source = "name")
    @Mapping(target = "eventDate", source = "date")
    @Mapping(target = "location", source = "place")
    @Mapping(target = "currency", source = "currency")
    TrivialEventFields toTrivialFields(EventDetails details);

    record TrivialEventFields(String name, LocalDate eventDate, String location, String currency) {
    }
}
