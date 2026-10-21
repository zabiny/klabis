package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.events.domain.Discipline;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface DisciplineDtoConverter extends Converter<Discipline, DisciplineDto> {

    @Override
    @Mapping(target = "id", source = "id.value")
    DisciplineDto convert(Discipline discipline);
}
