package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrisEventEntries cache expiry")
@SpringJUnitConfig
@ActiveProfiles("oris")
@Import(OrisEventEntriesTestConfig.class)
@TestPropertySource(properties = "test.oris-entries-cache-ttl-millis=300")
class OrisEventEntriesExpiryTest {

    private static final int CLUB_ID = OrisEventEntriesTestConfig.CLUB_ID;

    @Autowired
    private OrisApiClient orisApiClient;
    @Autowired
    private OrisEventEntries entries;

    @Test
    @DisplayName("calls ORIS again once the TTL has passed")
    void expires() {
        when(orisApiClient.getEventEntries(anyInt(), anyInt()))
                .thenReturn(new OrisApiClient.OrisResponse<>(Map.of(), "json", "OK", null, "getEventEntries"));

        entries.entriesOf(8123);

        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(50)).untilAsserted(() -> {
            entries.entriesOf(8123);
            verify(orisApiClient, times(2)).getEventEntries(8123, CLUB_ID);
        });
    }
}
