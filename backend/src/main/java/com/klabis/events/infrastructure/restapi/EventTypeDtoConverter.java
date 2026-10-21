package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.events.domain.EventType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import org.springframework.core.convert.converter.Converter;

import java.util.Optional;

@Mapper(config = MapstructSpringMapperConfig.class,
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
interface EventTypeDtoConverter extends Converter<EventType, EventTypeDto>, DisciplineIdMapping {

    @Override
    @Mapping(target = "id", source = "id.value")
    EventTypeDto convert(EventType eventType);

    default <T> T unwrap(Optional<T> value) {
        return value == null ? null : value.orElse(null);
    }
}
