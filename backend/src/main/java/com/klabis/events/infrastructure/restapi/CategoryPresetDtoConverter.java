package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.events.domain.CategoryPreset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface CategoryPresetDtoConverter extends Converter<CategoryPreset, CategoryPresetDto> {

    @Override
    @Mapping(target = "id", source = "id.value")
    CategoryPresetDto convert(CategoryPreset preset);
}
