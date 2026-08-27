package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.dto.EventDetails;
import com.klabis.common.sync.SyncData;

import java.util.Objects;

/**
 * External-side payload of the ORIS event {@code SyncLine}: the raw ORIS {@link EventDetails} plus
 * its numeric ORIS id. This is the last place the raw {@code com.dpolach.api.orisclient} type is
 * visible — {@link EventSyncDataConverter} translates it into the domain-shaped {@link EventSyncData}
 * before the local source ever sees it.
 */
public record OrisEventSyncData(int orisId, EventDetails details) implements SyncData {

    public OrisEventSyncData {
        Objects.requireNonNull(details, "ORIS event details required");
    }
}
