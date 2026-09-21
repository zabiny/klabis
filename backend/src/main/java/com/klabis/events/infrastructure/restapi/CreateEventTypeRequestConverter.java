package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.events.domain.EventType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface CreateEventTypeRequestConverter extends Converter<CreateEventTypeRequest, EventType.CreateEventType> {

    @Override
    @Mapping(target = "disciplineIds", expression = "java(EventTypeRequestConversions.toDisciplineIds(request.disciplineIds()))")
    EventType.CreateEventType convert(CreateEventTypeRequest request);
}
