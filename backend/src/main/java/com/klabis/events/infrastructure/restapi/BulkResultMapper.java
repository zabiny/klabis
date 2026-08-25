package com.klabis.events.infrastructure.restapi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ValueMapping;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
interface BulkResultMapper {

    BulkSyncResult toDto(com.klabis.events.application.BulkSyncResult result);

    @Mapping(target = "eventId", source = "eventId.value")
    EventSyncEntry toDto(com.klabis.events.application.BulkSyncResult.EventSyncEntry entry);

    @ValueMapping(source = "SYNCED", target = "SUCCESS")
    EventImportEntryStatus toDto(com.klabis.events.application.BulkSyncResult.SyncStatus status);

    BulkImportResult toDto(com.klabis.events.application.BulkImportResult result);

    EventImportEntry toDto(com.klabis.events.application.BulkImportResult.EventImportEntry entry);

    @ValueMapping(source = "IMPORTED", target = "SUCCESS")
    EventImportEntryStatus toDto(com.klabis.events.application.BulkImportResult.ImportStatus status);
}
