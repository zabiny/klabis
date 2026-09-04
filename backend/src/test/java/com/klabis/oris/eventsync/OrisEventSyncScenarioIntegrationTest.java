package com.klabis.oris.eventsync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventClass;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.EventDetailsBuilder;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.events.EventCategory;
import com.klabis.events.EventId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.OrisBulkSyncPort;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventUpdateEventBuilder;
import com.klabis.events.domain.Money;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncResolution;
import com.klabis.sync.domain.SyncStatus;
import com.klabis.sync.domain.SyncTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * End-to-end scenarios for events on the synchronisation engine (tasks.md 8.7, 8.8):
 * conflict raise/resolve/accept-divergence, field-ownership blindness to Klabis-owned
 * data, bulk sync skipping non-attemptable records, and retirement on finish.
 */
@SpringBootTest
@ActiveProfiles({"test", "oris"})
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Events on the sync engine — end-to-end scenarios")
class OrisEventSyncScenarioIntegrationTest {

    @Autowired
    private EventManagementPort eventManagementPort;

    @Autowired
    private OrisEventImportPort orisEventImportPort;

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private OrisBulkSyncPort orisBulkSyncPort;

    @MockitoBean
    private OrisApiClient orisApiClient;

    @MockitoBean
    private OrisWebUrls orisWebUrls;

    private static final AtomicInteger ORIS_ID_SEQUENCE = new AtomicInteger(655_000);

    private int orisId;
    private EventId eventId;
    private SyncRecord enrolled;

    @BeforeEach
    void setUp() {
        orisId = ORIS_ID_SEQUENCE.incrementAndGet();
        when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
        stubOrisEventDetails("Spring Sprint", null);

        Event imported = orisEventImportPort.importEventFromOris(orisId);
        eventId = imported.getId();
        enrolled = synchronizationPort.findByTarget(
                        new SyncTarget(SyncEntityType.EVENT, eventId.value().toString()))
                .orElseThrow();

        synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
    }

    @Test
    @DisplayName("importing an event from ORIS enrols it for synchronisation")
    void importEnrolsEventForSynchronisation() {
        SyncRecord record = synchronizationPort.findByTarget(
                        new SyncTarget(SyncEntityType.EVENT, eventId.value().toString()))
                .orElseThrow();

        assertThat(record.getTarget()).isEqualTo(new SyncTarget(SyncEntityType.EVENT, eventId.value().toString()));
        assertThat(record.getExternalReference().externalId()).isEqualTo(String.valueOf(orisId));
    }

    @Nested
    @DisplayName("Conflict lifecycle (task 8.7)")
    class ConflictLifecycle {

        @Test
        @DisplayName("a manager's edit raises a conflict and the name survives, before any resolution")
        void conflictingEditSurvivesUntilResolved() {
            editNameLocally("Manager's Correction");
            stubOrisEventDetails("ORIS Renamed It Too", null);

            SyncRecord afterConflict = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterConflict.getStatus()).isEqualTo(SyncStatus.CONFLICT);
            Event stillLocal = eventManagementPort.getEvent(eventId, true);
            assertThat(stillLocal.getName()).isEqualTo("Manager's Correction");
        }

        @Test
        @DisplayName("resolving inward restores the ORIS value")
        void resolvingInwardRestoresOrisValue() {
            editNameLocally("Manager's Correction");
            stubOrisEventDetails("ORIS Renamed It Too", null);

            SyncRecord afterConflict = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
            assertThat(afterConflict.getStatus()).isEqualTo(SyncStatus.CONFLICT);

            synchronizationPort.acknowledgeConflict(enrolled.getId(), "test-user");
            SyncRecord resolved = synchronizationPort.resolveConflict(enrolled.getId(), SyncResolution.INWARD, "test-user");

            assertThat(resolved.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            Event afterResolution = eventManagementPort.getEvent(eventId, true);
            assertThat(afterResolution.getName()).isEqualTo("ORIS Renamed It Too");
        }

        @Test
        @DisplayName("accepting divergence keeps the local edit and re-raises on the next ORIS change")
        void acceptingDivergenceKeepsEditAndReAsksLater() {
            editNameLocally("Manager's Correction");
            stubOrisEventDetails("ORIS Renamed It Too", null);

            SyncRecord afterConflict = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
            assertThat(afterConflict.getStatus()).isEqualTo(SyncStatus.CONFLICT);

            synchronizationPort.acknowledgeConflict(enrolled.getId(), "test-user");
            SyncRecord accepted = synchronizationPort.resolveConflict(enrolled.getId(), SyncResolution.ACCEPT_DIVERGENCE, "test-user");

            assertThat(accepted.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            Event afterAcceptance = eventManagementPort.getEvent(eventId, true);
            assertThat(afterAcceptance.getName()).isEqualTo("Manager's Correction");

            // The accepted divergence is a standing baseline, not a one-time pass: an
            // unrelated further ORIS change must surface a conflict again rather than
            // silently overwriting the accepted local edit (design.md D6, D7).
            stubOrisEventDetails("ORIS Renamed It Yet Again", null);
            SyncRecord afterNextChange = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterNextChange.getStatus()).isEqualTo(SyncStatus.CONFLICT);
            Event stillLocal = eventManagementPort.getEvent(eventId, true);
            assertThat(stillLocal.getName()).isEqualTo("Manager's Correction");
        }

        private void editNameLocally(String name) {
            Event event = eventManagementPort.getEvent(eventId, true);
            Event.UpdateEvent corrected = EventUpdateEventBuilder.builder(Event.UpdateEvent.from(event))
                    .name(name)
                    .build();
            eventManagementPort.updateEvent(eventId, corrected);
        }
    }

    @Nested
    @DisplayName("Field ownership (task 8.7): Klabis-owned data never causes a conflict")
    class FieldOwnership {

        @Test
        @DisplayName("a category fee override raises no conflict on the next sync")
        void feeOverrideRaisesNoConflict() {
            stubOrisEventDetails("Spring Sprint", Map.of("M21", mockClass("M21", "M21")));
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            Event event = eventManagementPort.getEvent(eventId, true);
            EventCategory m21 = event.getCategories().stream()
                    .filter(c -> "M21".equals(c.orisId()))
                    .findFirst().orElseThrow();
            EventCategory withOverride = new EventCategory(m21.id(), m21.orisId(), m21.name(), Money.ofCzk(new BigDecimal("500")));
            Event.UpdateEvent corrected = EventUpdateEventBuilder.builder(Event.UpdateEvent.from(event))
                    .categories(List.of(withOverride))
                    .build();
            eventManagementPort.updateEvent(eventId, corrected);

            // Same ORIS payload as the last pass compared against — the fee override is
            // the only local change, and it must not be visible to the projection.
            SyncRecord afterPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            Event stillOverridden = eventManagementPort.getEvent(eventId, true);
            EventCategory stillM21 = stillOverridden.getCategories().stream()
                    .filter(c -> "M21".equals(c.orisId()))
                    .findFirst().orElseThrow();
            assertThat(stillM21.feeOverride()).isEqualTo(Money.ofCzk(new BigDecimal("500")));
        }

        @Test
        @DisplayName("a manually added category raises no conflict on the next sync")
        void manuallyAddedCategoryRaisesNoConflict() {
            Event event = eventManagementPort.getEvent(eventId, true);
            Event.UpdateEvent withExtraCategory = EventUpdateEventBuilder.builder(Event.UpdateEvent.from(event))
                    .categories(List.of(EventCategory.create("Doprovodný závod")))
                    .build();
            eventManagementPort.updateEvent(eventId, withExtraCategory);

            SyncRecord afterPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            Event stillHasIt = eventManagementPort.getEvent(eventId, true);
            assertThat(stillHasIt.getCategories())
                    .extracting(EventCategory::name)
                    .contains("Doprovodný závod");
        }
    }

    @Nested
    @DisplayName("Bulk sync (task 8.8)")
    class BulkSync {

        @Test
        @DisplayName("skips and reports a conflicted event without attempting it")
        void skipsConflictedEvent() {
            Event event = eventManagementPort.getEvent(eventId, true);
            Event.UpdateEvent corrected = EventUpdateEventBuilder.builder(Event.UpdateEvent.from(event))
                    .name("Manager's Correction")
                    .build();
            eventManagementPort.updateEvent(eventId, corrected);
            stubOrisEventDetails("ORIS Renamed It Too", null);
            SyncRecord conflicted = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
            assertThat(conflicted.getStatus()).isEqualTo(SyncStatus.CONFLICT);

            var result = orisBulkSyncPort.syncAllUpcoming();

            assertThat(result.awaitingDecision())
                    .extracting(entry -> entry.eventId().value())
                    .contains(eventId.value());
        }

        @Test
        @DisplayName("skips and reports a terminally failed event without attempting it")
        void skipsTerminallyFailedEvent() {
            // Force a terminal failure: the mapper throws on a malformed ORIS payload
            // (a client/data-shaped error is TERMINAL per FailureClassifier), which the
            // engine records without the retry ladder ever getting a chance to recover.
            when(orisApiClient.getEventDetails(orisId)).thenThrow(new IllegalStateException("malformed ORIS payload"));
            SyncRecord failed = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
            assertThat(failed.getStatus()).isEqualTo(SyncStatus.FAILED);

            var result = orisBulkSyncPort.syncAllUpcoming();

            assertThat(result.stoppedByFailure())
                    .extracting(entry -> entry.eventId().value())
                    .contains(eventId.value());
        }
    }

    @Nested
    @DisplayName("Retirement (task 8.8)")
    class Retirement {

        @Test
        @DisplayName("a finished event is retired and no longer offered to the next bulk sync")
        void finishedEventIsRetiredAndExcludedFromBulkSync() {
            // finishExpiredActiveEvents only ever considers ACTIVE events (EventFilter
            // .activeEventsWithDateBefore) — a freshly imported event starts DRAFT.
            eventManagementPort.publishEvent(eventId);

            eventManagementPort.finishExpiredActiveEvents(LocalDate.now().plusYears(10));

            SyncRecord afterFinish = synchronizationPort.state(enrolled.getId());
            assertThat(afterFinish.getStatus()).isEqualTo(SyncStatus.RETIRED);

            var result = orisBulkSyncPort.syncAllUpcoming();

            assertThat(result.results())
                    .extracting(entry -> entry.eventId().value())
                    .doesNotContain(eventId.value());
            assertThat(result.awaitingDecision())
                    .extracting(entry -> entry.eventId().value())
                    .doesNotContain(eventId.value());
            assertThat(result.stoppedByFailure())
                    .extracting(entry -> entry.eventId().value())
                    .doesNotContain(eventId.value());
        }
    }

    private void stubOrisEventDetails(String name, Map<String, EventClass> classes) {
        EventDetailsBuilder builder = EventDetailsBuilder.builder()
                .name(name)
                .date(LocalDate.of(2026, 5, 1))
                .place("Brno Park")
                .org1(new Organizer(205, "OOB", "Orel Brno"));
        if (classes != null) {
            builder = builder.classes(classes);
        }
        EventDetails details = builder.build();

        when(orisApiClient.getEventDetails(orisId)).thenReturn(
                new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
    }

    private EventClass mockClass(String orisClassId, String name) {
        EventClass cls = Mockito.mock(EventClass.class);
        Mockito.lenient().when(cls.id()).thenReturn(orisClassId);
        Mockito.when(cls.name()).thenReturn(name);
        return cls;
    }
}
