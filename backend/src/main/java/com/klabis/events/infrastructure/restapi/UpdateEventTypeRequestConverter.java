package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.events.domain.EventType;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface UpdateEventTypeRequestConverter extends Converter<UpdateEventTypeRequest, EventType.UpdateEventType> {

    @Override
    EventType.UpdateEventType convert(UpdateEventTypeRequest request);
}
