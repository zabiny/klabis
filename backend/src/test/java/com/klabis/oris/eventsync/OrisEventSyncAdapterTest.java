package com.klabis.oris.eventsync;

import com.klabis.events.EventId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.OrisEventFields;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventFromOrisBuilder;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventSyncAdapter")
class OrisEventSyncAdapterTest {

    @Mock
    private EventManagementPort eventManagementPort;

    @Mock
    private OrisEventImportPort orisEventImportPort;

    private OrisEventSyncAdapter adapter;

    private static final UUID EVENT_UUID = UUID.randomUUID();
    private static final EventId EVENT_ID = new EventId(EVENT_UUID);
    private static final int ORIS_ID = 4242;

    @BeforeEach
    void setUp() {
        adapter = new OrisEventSyncAdapter(eventManagementPort, orisEventImportPort);
    }

    @Test
    @DisplayName("declares inward-only capabilities: no outward write, no create, no sensitive data")
    void declaresInwardOnlyCapabilities() {
        var capabilities = adapter.capabilities();

        assertThat(capabilities.readsLocal()).isTrue();
        assertThat(capabilities.readsExternal()).isTrue();
        assertThat(capabilities.writesLocal()).isTrue();
        assertThat(capabilities.writesExternal()).isFalse();
        assertThat(capabilities.createsLocal()).isFalse();
        assertThat(capabilities.createsExternal()).isFalse();
        assertThat(capabilities.containsSensitiveData()).isFalse();
    }

    @Test
    @DisplayName("entityType is EVENT and system is ORIS")
    void declaresEntityTypeAndSystem() {
        assertThat(adapter.entityType()).isEqualTo(SyncEntityType.EVENT);
        assertThat(adapter.system()).isEqualTo(com.klabis.sync.domain.ExternalSystem.ORIS);
    }

    @Nested
    @DisplayName("externalVersion()")
    class ExternalVersionMethod {

        @Test
        @DisplayName("is always empty — oris-client offers no cheap version signal, engine falls back to a full read")
        void alwaysEmpty() {
            assertThat(adapter.externalVersion("4242")).isEmpty();
        }
    }

    @Nested
    @DisplayName("readExternal()")
    class ReadExternalMethod {

        @Test
        @DisplayName("maps the ORIS fields read through OrisEventImportPort into the canonical projection")
        void mapsOrisFieldsIntoProjection() {
            OrisEventFields fields = new OrisEventFields(
                    "Spring Sprint", LocalDate.of(2026, 5, 1), "Brno Park", "OOB",
                    WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=4242"),
                    RegistrationDeadlines.none(), List.of(), null, null, null);
            when(orisEventImportPort.readOrisFields(ORIS_ID)).thenReturn(fields);

            SyncProjection projection = adapter.readExternal(String.valueOf(ORIS_ID));

            assertThat(projection).isInstanceOf(OrisEventProjection.class);
            assertThat(((OrisEventProjection) projection).name()).isEqualTo("Spring Sprint");
        }
    }

    @Nested
    @DisplayName("readLocal()")
    class ReadLocalMethod {

        @Test
        @DisplayName("maps the Klabis event read through EventManagementPort into the canonical projection")
        void mapsEventIntoProjection() {
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(ORIS_ID)
                    .name("Spring Sprint")
                    .eventDate(LocalDate.of(2026, 5, 1))
                    .location("Brno Park")
                    .organizer("OOB")
                    .websiteUrl(WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=4242"))
                    .build());
            when(eventManagementPort.getEvent(EVENT_ID, true)).thenReturn(event);

            SyncProjection projection = adapter.readLocal(EVENT_UUID.toString());

            assertThat(projection).isInstanceOf(OrisEventProjection.class);
            assertThat(((OrisEventProjection) projection).name()).isEqualTo("Spring Sprint");
        }
    }

    @Nested
    @DisplayName("applyToLocal()")
    class ApplyToLocalMethod {

        @Test
        @DisplayName("writes the projection inward via OrisEventImportPort.applyOrisSync")
        void writesInwardViaApplyOrisSync() {
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(ORIS_ID)
                    .name("Old name")
                    .eventDate(LocalDate.of(2026, 5, 1))
                    .location("Brno Park")
                    .organizer("OOB")
                    .websiteUrl(WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=4242"))
                    .build());
            when(eventManagementPort.getEvent(EVENT_ID, true)).thenReturn(event);
            when(orisEventImportPort.readOrisFields(ORIS_ID)).thenReturn(new OrisEventFields(
                    "New name", LocalDate.of(2026, 5, 1), "Brno Park", "OOB",
                    WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=4242"),
                    RegistrationDeadlines.none(), List.of(), null, null, null));

            OrisEventProjection incoming = new OrisEventProjection(
                    "New name", LocalDate.of(2026, 5, 1), "Brno Park", "OOB",
                    "https://oris.ceskyorientak.cz/Zavod?id=4242",
                    null, null, null, List.of(), null, null, null, null, null);

            adapter.applyToLocal(EVENT_UUID.toString(), incoming);

            ArgumentCaptor<OrisEventFields> captor = ArgumentCaptor.forClass(OrisEventFields.class);
            verify(orisEventImportPort).applyOrisSync(eq(EVENT_ID), captor.capture());
            assertThat(captor.getValue().name()).isEqualTo("New name");
        }

    }

    @Nested
    @DisplayName("applyToExternal()")
    class ApplyToExternalMethod {

        @Test
        @DisplayName("throws — the adapter declares no outward write capability")
        void throwsUnsupported() {
            OrisEventProjection projection = new OrisEventProjection(
                    "Name", LocalDate.now(), "Loc", "Org", null,
                    null, null, null, List.of(), null, null, null, null, null);

            assertThatThrownBy(() -> adapter.applyToExternal("4242", projection))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
