package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.events.domain.EventType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface EventTypeDtoConverter extends Converter<EventType, EventTypeDto> {

    @Override
    @Mapping(target = "id", expression = "java(eventType.getId().value())")
    @Mapping(target = "color", expression = "java(eventType.getColor().orElse(null))")
    @Mapping(target = "disciplineIds", expression = "java(EventTypeRequestConversions.toDisciplineUuids(eventType.getDisciplineIds()))")
    EventTypeDto convert(EventType eventType);
}
