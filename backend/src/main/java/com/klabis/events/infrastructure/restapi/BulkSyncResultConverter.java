package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * Implements Spring's {@link Converter} (rather than a plain {@code @Mapper}) so {@code @WebMvcTest}
 * slices pick it up automatically — {@code WebMvcTypeExcludeFilter} always lets {@link Converter}
 * beans through, regardless of the test's {@code controllers} filter. See {@code MonetaryAmountConverter}
 * for the precedent.
 */
@Mapper(config = MapstructSpringMapperConfig.class, uses = BulkResultMapper.class)
interface BulkSyncResultConverter extends Converter<com.klabis.events.application.BulkSyncResult, BulkSyncResult> {

    @Override
    BulkSyncResult convert(com.klabis.events.application.BulkSyncResult source);
}
