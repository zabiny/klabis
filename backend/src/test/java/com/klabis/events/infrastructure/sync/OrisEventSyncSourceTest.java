package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.klabis.common.sync.SyncItemId;
import com.klabis.common.sync.SyncParty;
import com.klabis.common.sync.SyncType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventSyncSource")
class OrisEventSyncSourceTest {

    @Mock
    private OrisApiClient orisApiClient;

    @Mock
    private OrisWebUrls orisWebUrls;

    private OrisEventSyncSource testedInstance;

    @BeforeEach
    void setUp() {
        testedInstance = new OrisEventSyncSource(orisApiClient, orisWebUrls);
    }

    @Test
    @DisplayName("type() is EVENT and party() is EXTERNAL")
    void identity() {
        assertThat(testedInstance.type()).isEqualTo(SyncType.EVENT);
        assertThat(testedInstance.party()).isEqualTo(SyncParty.EXTERNAL);
    }

    @Test
    @DisplayName("fetch() maps the ORIS EventDetails payload into EventSyncData")
    void fetch_mapsPayload() {
        int orisId = 123;
        EventDetails details = Mockito.mock(EventDetails.class);
        when(orisApiClient.getEventDetails(orisId)).thenReturn(
                new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
        when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

        Optional<EventSyncData> result = testedInstance.fetch(SyncItemId.externalId(SyncType.EVENT, "123"));

        assertThat(result).isPresent();
        assertThat(result.get().eventId()).isNull();
        assertThat(result.get().orisId()).isEqualTo(orisId);
        assertThat(result.get().orisDetails()).isSameAs(details);
        assertThat(result.get().eventWebUrl()).isEqualTo("https://oris.ceskyorientak.cz/Zavod?id=123");
    }

    @Test
    @DisplayName("fetch() returns empty when the ORIS payload is empty")
    void fetch_emptyPayload() {
        int orisId = 999;
        when(orisApiClient.getEventDetails(orisId)).thenReturn(
                new OrisApiClient.OrisResponse<>(null, "JSON", "OK", null, "getEvent"));

        assertThat(testedInstance.fetch(SyncItemId.externalId(SyncType.EVENT, "999"))).isEmpty();
    }

    @Test
    @DisplayName("save() throws OrisEventSaveNotSupportedException — ORIS is read-only")
    void save_throws() {
        EventSyncData data = new EventSyncData(null, 555, null, null);

        assertThatThrownBy(() -> testedInstance.save(data))
                .isInstanceOf(OrisEventSaveNotSupportedException.class)
                .hasMessageContaining("read-only")
                .hasMessageContaining("555");
    }
}
