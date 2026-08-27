package com.klabis.events.application;

import com.klabis.common.sync.SyncItemId;
import com.klabis.common.sync.SyncType;
import com.klabis.events.EventId;

import java.util.UUID;

/**
 * Translations between Klabis {@link EventId} / ORIS id and the engine's {@link SyncItemId} for
 * {@link SyncType#EVENT}. Local {@code SyncId}s carry the Klabis event UUID; external ones carry
 * the ORIS event id as a string.
 */
public final class EventSyncIds {

    private EventSyncIds() {
    }

    public static EventId toEventId(SyncItemId localSyncItemId) {
        return new EventId(UUID.fromString(localSyncItemId.idValue()));
    }

    public static SyncItemId localSyncId(EventId eventId) {
        return SyncItemId.localId(SyncType.EVENT, eventId.value().toString());
    }

    public static SyncItemId externalSyncId(int orisId) {
        return SyncItemId.externalId(SyncType.EVENT, Integer.toString(orisId));
    }

    public static int toOrisId(SyncItemId externalSyncItemId) {
        return Integer.parseInt(externalSyncItemId.idValue());
    }
}
