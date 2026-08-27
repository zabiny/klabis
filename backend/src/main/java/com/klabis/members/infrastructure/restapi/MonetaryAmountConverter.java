package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * Implements Spring's {@link Converter} (rather than a plain {@code @Mapper}) so {@code @WebMvcTest}
 * slices pick it up automatically — {@code WebMvcTypeExcludeFilter} always lets {@link Converter}
 * beans through, regardless of the test's {@code controllers} filter. This is what
 * {@link MembersExceptionHandler}, a global {@code @RestControllerAdvice}, relies on via
 * {@code ConversionService} without needing test-side wiring.
 */
@Mapper(config = MapstructSpringMapperConfig.class)
interface MonetaryAmountConverter extends Converter<com.klabis.members.MonetaryAmount, MonetaryAmount> {

    @Override
    MonetaryAmount convert(com.klabis.members.MonetaryAmount source);
}
