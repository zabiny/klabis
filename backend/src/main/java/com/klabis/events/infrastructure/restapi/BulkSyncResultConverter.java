package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
interface BulkSyncResultConverter extends Converter<com.klabis.events.application.BulkSyncResult, BulkSyncResult> {

    /**
     * The four count fields are derived accessors on the domain record, not record
     * components, so MapStruct's record-component-based auto-mapping does not see
     * them as mapping sources — they must be wired explicitly via
     * {@code expression}.
     */
    @Override
    @Mapping(target = "successCount", expression = "java(source.successCount())")
    @Mapping(target = "failureCount", expression = "java(source.failureCount())")
    @Mapping(target = "awaitingDecisionCount", expression = "java(source.awaitingDecisionCount())")
    @Mapping(target = "stoppedByFailureCount", expression = "java(source.stoppedByFailureCount())")
    BulkSyncResult convert(com.klabis.events.application.BulkSyncResult source);

    @Mapping(target = "eventId", source = "eventId.value")
    EventSyncEntry toDto(com.klabis.events.application.BulkSyncResult.EventSyncEntry entry);

    @ValueMapping(source = "SYNCED", target = "SUCCESS")
    EventSyncEntryStatus toDto(com.klabis.events.application.BulkSyncResult.SyncStatus status);
}
