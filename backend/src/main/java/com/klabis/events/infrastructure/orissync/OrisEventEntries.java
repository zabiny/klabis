package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.autoconfigure.OrisClientProperties;
import com.dpolach.api.orisclient.dto.EventEntry;
import com.klabis.common.OrisIntegrationComponent;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The club's ORIS entries of an event, cached per ORIS event for a short TTL.
 * <p>
 * {@code oris-client} does not cache {@code getEventEntries}. A TTL rather than a per-run cache because engine
 * scans are not runs this class can see begin and end: a scan over N registrations of one event, and a discovery
 * run followed by the passes it triggers, should cost one ORIS call.
 */
@OrisIntegrationComponent
public class OrisEventEntries {

    private record Cached(List<EventEntry> entries, Instant readAt) {
    }

    private final OrisApiClient orisApiClient;
    private final OrisClientProperties orisClientProperties;
    private final OrisEntriesProperties properties;
    private final Clock clock;
    private final Map<Integer, Cached> cache = new ConcurrentHashMap<>();

    OrisEventEntries(OrisApiClient orisApiClient, OrisClientProperties orisClientProperties,
                     OrisEntriesProperties properties, Clock clock) {
        this.orisApiClient = orisApiClient;
        this.orisClientProperties = orisClientProperties;
        this.properties = properties;
        this.clock = clock;
    }

    public List<EventEntry> entriesOf(int orisEventId) {
        Instant now = clock.instant();
        Cached cached = cache.get(orisEventId);
        if (cached != null && now.isBefore(cached.readAt().plus(properties.getOrisEntriesCacheTtl()))) {
            return cached.entries();
        }
        List<EventEntry> fresh = read(orisEventId);
        cache.put(orisEventId, new Cached(fresh, now));
        return fresh;
    }

    public void evict(int orisEventId) {
        cache.remove(orisEventId);
    }

    private List<EventEntry> read(int orisEventId) {
        Integer clubId = orisClientProperties.myClubId();
        if (clubId == null) {
            throw new IllegalStateException("oris.client.my-club-id is required to read ORIS event entries");
        }
        return orisApiClient.getEventEntries(orisEventId, clubId).payload()
                .map(byId -> List.copyOf(new ArrayList<>(byId.values())))
                .orElseGet(List::of);
    }
}
