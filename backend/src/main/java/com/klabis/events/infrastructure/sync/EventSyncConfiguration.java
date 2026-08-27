package com.klabis.events.infrastructure.sync;

import com.klabis.common.sync.SyncLine;
import com.klabis.oris.OrisIntegrationComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the ORIS event {@code SyncLine}. Profile-gated ({@code oris}) via
 * {@link OrisIntegrationComponent} because {@link OrisEventSyncSource} needs the ORIS API client.
 */
@Configuration
@OrisIntegrationComponent
class EventSyncConfiguration {

    @Bean
    SyncLine<EventSyncData, EventSyncData> orisEventSyncLine(
            LocalEventSyncSource local, OrisEventSyncSource oris, IdentityEventConverter identity) {
        return new SyncLine<>(local, oris, identity, identity);
    }
}
