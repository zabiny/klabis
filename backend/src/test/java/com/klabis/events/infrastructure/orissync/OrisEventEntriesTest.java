package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisApiClient.OrisResponse;
import com.dpolach.api.orisclient.autoconfigure.OrisClientProperties;
import com.dpolach.api.orisclient.dto.EventEntry;
import com.dpolach.api.orisclient.dto.EventEntryBuilder;
import com.klabis.sync.fixtures.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrisEventEntries")
class OrisEventEntriesTest {

    private static final int CLUB_ID = 205;

    private final OrisApiClient orisApiClient = mock(OrisApiClient.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-01T10:00:00Z"), ZoneId.of("UTC"));
    private OrisEventEntries entries;

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
        OrisEntriesProperties properties = new OrisEntriesProperties();
        properties.setOrisEntriesCacheTtl(Duration.ofMinutes(5));
        entries = new OrisEventEntries(orisApiClient, orisProperties(CLUB_ID), properties, clock);
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
        clock.advanceBy(Duration.ofMinutes(4));
        entries.entriesOf(8123);

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
    @DisplayName("calls ORIS again once the TTL has passed")
    void expires() {
        entries.entriesOf(8123);
        clock.advanceBy(Duration.ofMinutes(6));
        entries.entriesOf(8123);

        verify(orisApiClient, times(2)).getEventEntries(8123, CLUB_ID);
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
        OrisEventEntries withoutClub = new OrisEventEntries(orisApiClient, orisProperties(null),
                new OrisEntriesProperties(), clock);

        assertThatThrownBy(() -> withoutClub.entriesOf(8123))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("my-club-id");
    }
}
