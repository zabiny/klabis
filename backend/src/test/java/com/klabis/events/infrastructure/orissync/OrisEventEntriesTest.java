package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisApiClient.OrisResponse;
import com.dpolach.api.orisclient.autoconfigure.OrisClientProperties;
import com.dpolach.api.orisclient.dto.EventEntry;
import com.dpolach.api.orisclient.dto.EventEntryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.net.URI;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrisEventEntries")
@SpringJUnitConfig
@ActiveProfiles("oris")
@Import(OrisEventEntriesTestConfig.class)
@TestPropertySource(properties = "test.oris-entries-cache-ttl-millis=2000")
class OrisEventEntriesTest {

    private static final int CLUB_ID = 205;

    @Autowired
    private OrisApiClient orisApiClient;
    @Autowired
    private OrisEventEntries entries;
    @Autowired
    private CacheManager cacheManager;

    private static OrisClientProperties orisProperties(Integer clubId) {
        return new OrisClientProperties(true, URI.create("https://oris.example"), URI.create("https://oris.example"),
                ZoneId.of("Europe/Prague"), "yyyy-MM-dd HH:mm:ss", clubId);
    }

    private static OrisResponse<Map<String, EventEntry>> response(EventEntry... entries) {
        Map<String, EventEntry> byId = new java.util.LinkedHashMap<>();
        for (EventEntry entry : entries) {
            byId.put(String.valueOf(entry.id()), entry);
        }
        return new OrisResponse<>(byId, "json", "OK", null, "getEventEntries");
    }

    private static EventEntry entry(int id, Integer clubUserId) {
        return EventEntryBuilder.builder().id(id).clubUserId(clubUserId).build();
    }

    @BeforeEach
    void setUp() {
        cacheManager.getCache(OrisEventEntries.CACHE_NAME).clear();
        reset(orisApiClient);
        when(orisApiClient.getEventEntries(anyInt(), anyInt())).thenReturn(response(entry(1, 11), entry(2, 12)));
    }

    @Test
    @DisplayName("asks ORIS for the club's entries of the event")
    void readsClubEntries() {
        List<EventEntry> result = entries.entriesOf(8123);

        assertThat(result).extracting(EventEntry::id).containsExactly(1, 2);
        verify(orisApiClient).getEventEntries(8123, CLUB_ID);
    }

    @Test
    @DisplayName("calls ORIS once for N reads of one event within the TTL")
    void cachesWithinTtl() {
        for (int i = 0; i < 5; i++) {
            entries.entriesOf(8123);
        }

        verify(orisApiClient, times(1)).getEventEntries(8123, CLUB_ID);
    }

    @Test
    @DisplayName("keeps events apart")
    void cachesPerEvent() {
        entries.entriesOf(1);
        entries.entriesOf(2);

        verify(orisApiClient, times(1)).getEventEntries(1, CLUB_ID);
        verify(orisApiClient, times(1)).getEventEntries(2, CLUB_ID);
    }

    @Test
    @DisplayName("eviction of one event forces a new call for that event only")
    void evictionForcesNewCall() {
        entries.entriesOf(1);
        entries.entriesOf(2);

        entries.evict(1);
        entries.entriesOf(1);
        entries.entriesOf(2);

        verify(orisApiClient, times(2)).getEventEntries(1, CLUB_ID);
        verify(orisApiClient, times(1)).getEventEntries(2, CLUB_ID);
    }

    @Test
    @DisplayName("an event with no entries yields an empty list")
    void emptyPayload() {
        when(orisApiClient.getEventEntries(anyInt(), anyInt()))
                .thenReturn(new OrisResponse<>(null, "json", "OK", null, "getEventEntries"));

        assertThat(entries.entriesOf(8123)).isEmpty();
    }

    @Test
    @DisplayName("does not cache a failed read")
    void failureNotCached() {
        when(orisApiClient.getEventEntries(anyInt(), anyInt()))
                .thenReturn(new OrisResponse<>(null, "json", "ERROR", null, "getEventEntries"))
                .thenReturn(response(entry(1, 11)));

        assertThatThrownBy(() -> entries.entriesOf(8123)).isInstanceOf(IllegalStateException.class);
        assertThat(entries.entriesOf(8123)).hasSize(1);
    }

    @Test
    @DisplayName("fails when no ORIS club id is configured")
    void requiresClubId() {
        OrisEventEntries withoutClub = new OrisEventEntries(orisApiClient, orisProperties(null));

        assertThatThrownBy(() -> withoutClub.entriesOf(8123))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("my-club-id");
    }
}
