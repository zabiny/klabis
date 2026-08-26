package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.ValueMapping;
import org.springframework.core.convert.converter.Converter;

/**
 * Implements Spring's {@link Converter} (rather than a plain {@code @Mapper}) so {@code @WebMvcTest}
 * slices pick it up automatically — {@code WebMvcTypeExcludeFilter} always lets {@link Converter}
 * beans through, regardless of the test's {@code controllers} filter. See {@code MonetaryAmountConverter}
 * for the precedent.
 * <p>
 * Nested entry/status mapping is declared directly on this interface (not shared via {@code uses = }
 * another mapper) because {@code Converter} beans are visible to every {@code @WebMvcTest} slice in the
 * app regardless of its {@code controllers} filter — a {@code uses} dependency on a plain, non-Converter
 * {@code @Mapper} would force every unrelated slice to import that mapper's generated impl too.
 */
@Mapper(config = MapstructSpringMapperConfig.class)
interface BulkImportResultConverter extends Converter<com.klabis.events.application.BulkImportResult, BulkImportResult> {

    @Override
    BulkImportResult convert(com.klabis.events.application.BulkImportResult source);

    EventImportEntry toDto(com.klabis.events.application.BulkImportResult.EventImportEntry entry);

    @ValueMapping(source = "IMPORTED", target = "SUCCESS")
    EventImportEntryStatus toDto(com.klabis.events.application.BulkImportResult.ImportStatus status);
}
