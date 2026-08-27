package com.klabis.events.infrastructure.sync;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Identity converter for both directions of the ORIS event {@code SyncLine}: {@code L} and {@code E}
 * are the same type ({@link EventSyncData}), so nothing needs converting. A future {@code SyncLine}
 * refactor may drop the converter from the pipeline entirely.
 */
@Component
class IdentityEventConverter implements Converter<EventSyncData, EventSyncData> {

    @Override
    public EventSyncData convert(EventSyncData source) {
        return source;
    }
}
