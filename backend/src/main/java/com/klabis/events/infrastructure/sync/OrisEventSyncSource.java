package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.klabis.common.sync.SyncId;
import com.klabis.common.sync.SyncParty;
import com.klabis.common.sync.SyncSource;
import com.klabis.common.sync.SyncType;
import com.klabis.events.application.EventSyncIds;
import com.klabis.oris.OrisIntegrationComponent;

import java.util.Optional;

/**
 * External ({@code party=EXTERNAL}) side of the ORIS event {@code SyncLine}. Read-only: {@code fetch}
 * pulls an ORIS event payload, {@code save} always throws — ORIS accepts no writes from Klabis.
 */
@OrisIntegrationComponent
class OrisEventSyncSource implements SyncSource<EventSyncData> {

    private final OrisApiClient orisApiClient;
    private final OrisWebUrls orisWebUrls;

    OrisEventSyncSource(OrisApiClient orisApiClient, OrisWebUrls orisWebUrls) {
        this.orisApiClient = orisApiClient;
        this.orisWebUrls = orisWebUrls;
    }

    @Override
    public Optional<EventSyncData> fetch(SyncId syncId) {
        int orisId = EventSyncIds.toOrisId(syncId);
        return orisApiClient.getEventDetails(orisId).payload()
                .map(details -> new EventSyncData(null, orisId, details, orisWebUrls.eventUrl(orisId)));
    }

    @Override
    public SyncId save(EventSyncData data) {
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
