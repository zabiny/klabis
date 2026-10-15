package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.autoconfigure.OrisClientProperties;
import com.dpolach.api.orisclient.dto.EventEntry;
import com.klabis.common.OrisIntegrationComponent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * The club's ORIS entries of an event, cached per ORIS event for a short TTL.
 * <p>
 * {@code oris-client} does not cache {@code getEventEntries}. A TTL rather than a per-run cache because engine
 * scans are not runs this class can see begin and end: a scan over N registrations of one event, and a discovery
 * run followed by the passes it triggers, should cost one ORIS call.
 */
@OrisIntegrationComponent
public class OrisEventEntries {

    static final String CACHE_NAME = "orisEventEntries";

    private final OrisApiClient orisApiClient;
    private final OrisClientProperties orisClientProperties;

    OrisEventEntries(OrisApiClient orisApiClient, OrisClientProperties orisClientProperties) {
        this.orisApiClient = orisApiClient;
        this.orisClientProperties = orisClientProperties;
    }

    @Cacheable(CACHE_NAME)
    public List<EventEntry> entriesOf(int orisEventId) {
        return read(orisEventId);
    }

    @CacheEvict(CACHE_NAME)
    public void evict(int orisEventId) {
    }

    private List<EventEntry> read(int orisEventId) {
        Integer clubId = orisClientProperties.myClubId();
        if (clubId == null) {
            throw new IllegalStateException("oris.client.my-club-id is required to read ORIS event entries");
        }
        return orisApiClient.getEventEntries(orisEventId, clubId).payload()
                .map(byId -> List.copyOf(byId.values()))
                .orElseGet(List::of);
    }
}
