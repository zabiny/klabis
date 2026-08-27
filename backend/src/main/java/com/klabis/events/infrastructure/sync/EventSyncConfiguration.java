package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.OrisWebUrls;
import com.klabis.common.sync.SyncLine;
import com.klabis.oris.OrisIntegrationComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the ORIS event {@code SyncLine}. Profile-gated ({@code oris}) via
 * {@link OrisIntegrationComponent} because {@link OrisEventSyncSource} needs the ORIS API client.
 * <p>
 * The forward (PUSH) converter throws: ORIS is a read-only source for Klabis. The reverse (PULL)
 * converter is {@link EventSyncDataConverter}, which owns the raw ORIS DTO translation.
 */
@Configuration
@OrisIntegrationComponent
class EventSyncConfiguration {

    @Bean
    SyncLine<EventSyncData, OrisEventSyncData> orisEventSyncLine(
            LocalEventSyncSource klabisDataSource,
            OrisEventSyncSource orisDataSource,
            OrisEventDetailsMapper detailsMapper,
            OrisEventMappingSupport mappingSupport,
            OrisWebUrls orisWebUrls) {
        EventSyncDataConverter reverseConverter =
                new EventSyncDataConverter(detailsMapper, mappingSupport, orisWebUrls);
        return new SyncLine<>(
                klabisDataSource,
                orisDataSource,
                localData -> {
                    throw new UnsupportedOperationException(
                            "ORIS is a read-only sync source; PUSH for EVENT is not supported");
                },
                reverseConverter);
    }
}
