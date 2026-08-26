package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
public interface MonetaryAmountConverter extends Converter<com.klabis.members.MonetaryAmount, MonetaryAmount> {

    @Override
    MonetaryAmount convert(com.klabis.members.MonetaryAmount source);
}
