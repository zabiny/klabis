package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.dto.EventDetails;
import com.klabis.common.sync.SyncData;
import com.klabis.events.EventId;

/**
 * The single payload type that flows both ways across the ORIS event {@code SyncLine}.
 * <p>
 * {@code eventId} is {@code null} until a local Klabis event exists (import path, and the resync
 * path where only the ORIS payload has been fetched). {@code orisDetails} is the raw ORIS payload
 * and is {@code null} on the local {@code fetch} path (PUSH), which ORIS does not support anyway.
 */
public record EventSyncData(EventId eventId, Integer orisId, EventDetails orisDetails, String eventWebUrl)
        implements SyncData {

    public EventSyncData {
        if (eventId == null && orisId == null) {
            throw new IllegalArgumentException(
                    "EventSyncData requires at least one of eventId / orisId");
        }
    }

}
