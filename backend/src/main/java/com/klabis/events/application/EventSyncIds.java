package com.klabis.events.application;

import com.klabis.common.sync.SyncId;
import com.klabis.common.sync.SyncType;
import com.klabis.events.EventId;

import java.util.UUID;

/**
 * Translations between Klabis {@link EventId} / ORIS id and the engine's {@link SyncId} for
 * {@link SyncType#EVENT}. Local {@code SyncId}s carry the Klabis event UUID; external ones carry
 * the ORIS event id as a string.
 */
public final class EventSyncIds {

    private EventSyncIds() {
    }

    public static EventId toEventId(SyncId localSyncId) {
        return new EventId(UUID.fromString(localSyncId.idValue()));
    }

    public static SyncId localSyncId(EventId eventId) {
        return SyncId.localId(SyncType.EVENT, eventId.value().toString());
    }

    public static SyncId externalSyncId(int orisId) {
        return SyncId.externalId(SyncType.EVENT, Integer.toString(orisId));
    }

    public static int toOrisId(SyncId externalSyncId) {
        return Integer.parseInt(externalSyncId.idValue());
    }
}
