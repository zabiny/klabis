package com.klabis.events.infrastructure.restapi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ValueMapping;

/**
 * Internal helper mapper for the nested entry/status mapping used by
 * {@link BulkSyncResultConverter} and {@link BulkImportResultConverter}.
 * Not injected directly — MapStruct wires it in via {@code uses = BulkResultMapper.class}.
 */
@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
interface BulkResultMapper {

    @Mapping(target = "eventId", source = "eventId.value")
    EventSyncEntry toDto(com.klabis.events.application.BulkSyncResult.EventSyncEntry entry);

    @ValueMapping(source = "SYNCED", target = "SUCCESS")
    EventImportEntryStatus toDto(com.klabis.events.application.BulkSyncResult.SyncStatus status);

    EventImportEntry toDto(com.klabis.events.application.BulkImportResult.EventImportEntry entry);

    @ValueMapping(source = "IMPORTED", target = "SUCCESS")
    EventImportEntryStatus toDto(com.klabis.events.application.BulkImportResult.ImportStatus status);
}
