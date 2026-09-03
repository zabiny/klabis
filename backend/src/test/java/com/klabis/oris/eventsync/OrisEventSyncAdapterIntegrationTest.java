package com.klabis.oris.eventsync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.EventDetailsBuilder;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.events.EventId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventUpdateEventBuilder;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncStatus;
import com.klabis.sync.domain.SyncTarget;
import com.klabis.sync.domain.SyncEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link OrisEventSyncAdapter} wired into the real
 * synchronisation engine against a stubbed ORIS client (tasks.md 7.6).
 */
@SpringBootTest
@ActiveProfiles({"test", "oris"})
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("OrisEventSyncAdapter — wired into the synchronisation engine")
class OrisEventSyncAdapterIntegrationTest {

    @Autowired
    private EventManagementPort eventManagementPort;

    @Autowired
    private OrisEventImportPort orisEventImportPort;

    @Autowired
    private SynchronizationPort synchronizationPort;

    @MockitoBean
    private OrisApiClient orisApiClient;

    @MockitoBean
    private OrisWebUrls orisWebUrls;

    private static final java.util.concurrent.atomic.AtomicInteger ORIS_ID_SEQUENCE =
            new java.util.concurrent.atomic.AtomicInteger(555_000);

    private int orisId;
    private EventId eventId;

    @BeforeEach
    void setUp() {
        orisId = ORIS_ID_SEQUENCE.incrementAndGet();
        when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
        stubOrisEventDetails("Spring Sprint");

        Event imported = orisEventImportPort.importEventFromOris(orisId);
        eventId = imported.getId();
    }

    @Test
    @DisplayName("an external change is written inward when nothing changed locally")
    void inwardWriteOnExternalChange() {
        SyncRecord enrolled = synchronizationPort.enroll(
                new SyncTarget(SyncEntityType.EVENT, eventId.value().toString()),
                new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId)));
        synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        stubOrisEventDetails("Spring Sprint Renamed");
        SyncRecord afterPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        assertThat(afterPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        Event updated = eventManagementPort.getEvent(eventId, true);
        assertThat(updated.getName()).isEqualTo("Spring Sprint Renamed");
    }

    @Test
    @DisplayName("a local edit to an ORIS-owned field raises a conflict instead of being overwritten")
    void localEditToOrisOwnedFieldRaisesConflict() {
        SyncRecord enrolled = synchronizationPort.enroll(
                new SyncTarget(SyncEntityType.EVENT, eventId.value().toString()),
                new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId)));
        synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        // Simulate a manager's local edit to an ORIS-owned field (the name) by
        // re-syncing with a different local value applied directly, bypassing ORIS —
        // equivalent to a manual correction that the next pass must not discard.
        Event event = eventManagementPort.getEvent(eventId, true);
        Event.UpdateEvent corrected = EventUpdateEventBuilder.builder(Event.UpdateEvent.from(event))
                .name("Manager's Correction")
                .build();
        eventManagementPort.updateEvent(eventId, corrected);

        stubOrisEventDetails("ORIS Renamed It Too");
        SyncRecord afterPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        assertThat(afterPass.getStatus()).isEqualTo(SyncStatus.CONFLICT);
        Event stillLocal = eventManagementPort.getEvent(eventId, true);
        assertThat(stillLocal.getName()).isEqualTo("Manager's Correction");
    }

    @Test
    @DisplayName("externalVersion() never yields a token, so every pass performs a full external read")
    void everyPassFallsBackToFullRead() {
        SyncRecord enrolled = synchronizationPort.enroll(
                new SyncTarget(SyncEntityType.EVENT, eventId.value().toString()),
                new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId)));
        synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        Mockito.clearInvocations(orisApiClient);
        synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        Mockito.verify(orisApiClient, Mockito.atLeastOnce()).getEventDetails(orisId);
    }

    private void stubOrisEventDetails(String name) {
        EventDetails details = EventDetailsBuilder.builder()
                .name(name)
                .date(LocalDate.of(2026, 5, 1))
                .place("Brno Park")
                .org1(new Organizer(205, "OOB", "Orel Brno"))
                .build();

        when(orisApiClient.getEventDetails(orisId)).thenReturn(
                new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
    }
}
