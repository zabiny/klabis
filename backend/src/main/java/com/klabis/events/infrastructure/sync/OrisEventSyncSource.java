package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.klabis.common.sync.SyncItemId;
import com.klabis.common.sync.SyncParty;
import com.klabis.common.sync.SyncSource;
import com.klabis.common.sync.SyncType;
import com.klabis.events.application.EventSyncIds;
import com.klabis.oris.OrisIntegrationComponent;

import java.util.Optional;

/**
 * External ({@code party=EXTERNAL}) side of the ORIS event {@code SyncLine}. Read-only: {@code fetch}
 * pulls a raw ORIS event payload ({@link OrisEventSyncData}), {@code save} always throws — ORIS
 * accepts no writes from Klabis. All translation of the raw ORIS DTO happens in
 * {@link EventSyncDataConverter}, not here.
 */
@OrisIntegrationComponent
class OrisEventSyncSource implements SyncSource<OrisEventSyncData> {

    private final OrisApiClient orisApiClient;

    OrisEventSyncSource(OrisApiClient orisApiClient) {
        this.orisApiClient = orisApiClient;
    }

    @Override
    public Optional<OrisEventSyncData> fetch(SyncItemId syncItemId) {
        int orisId = EventSyncIds.toOrisId(syncItemId);
        return orisApiClient.getEventDetails(orisId).payload()
                .map(details -> new OrisEventSyncData(orisId, details));
    }

    @Override
    public SyncItemId save(OrisEventSyncData data) {
        throw new OrisEventSaveNotSupportedException(data.orisId());
    }

    @Override
    public SyncType type() {
        return SyncType.EVENT;
    }

    @Override
    public SyncParty party() {
        return SyncParty.EXTERNAL;
    }
}
