package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.events.domain.Discipline;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface CreateDisciplineRequestConverter extends Converter<CreateDisciplineRequest, Discipline.CreateDiscipline> {

    @Override
    Discipline.CreateDiscipline convert(CreateDisciplineRequest request);
}
