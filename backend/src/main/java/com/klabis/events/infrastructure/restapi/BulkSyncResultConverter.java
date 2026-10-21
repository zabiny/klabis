package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ValueMapping;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

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
     * The counts are derived accessors on the domain record, not record components, so MapStruct
     * does not see them as properties.
     */
    @Override
    default BulkSyncResult convert(com.klabis.events.application.BulkSyncResult source) {
        return BulkSyncResultBuilder.builder()
                .totalProcessed(source.totalProcessed())
                .successCount(source.successCount())
                .failureCount(source.failureCount())
                .awaitingDecisionCount(source.awaitingDecisionCount())
                .stoppedByFailureCount(source.stoppedByFailureCount())
                .results(toDtos(source.results()))
                .awaitingDecision(toDtos(source.awaitingDecision()))
                .stoppedByFailure(toDtos(source.stoppedByFailure()))
                .build();
    }

    List<EventSyncEntry> toDtos(List<com.klabis.events.application.BulkSyncResult.EventSyncEntry> entries);

    @Mapping(target = "eventId", source = "eventId.value")
    EventSyncEntry toDto(com.klabis.events.application.BulkSyncResult.EventSyncEntry entry);

    @ValueMapping(source = "SYNCED", target = "SUCCESS")
    EventSyncEntryStatus toDto(com.klabis.events.application.BulkSyncResult.SyncStatus status);
}
