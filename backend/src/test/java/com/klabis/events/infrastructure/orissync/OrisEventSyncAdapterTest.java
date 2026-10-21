package com.klabis.events.infrastructure.orissync;

import com.klabis.events.EventCategory;
import com.klabis.events.EventCategoryId;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventFromOrisBuilder;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.members.MemberId;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.events.EventTestDataBuilder.register;
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
    private OrisEventDetailsReader orisEventDetailsReader;

    @Mock
    private EventRepository eventRepository;

    private OrisEventSyncAdapter adapter;

    private static final UUID EVENT_UUID = UUID.randomUUID();
    private static final EventId EVENT_ID = new EventId(EVENT_UUID);
    private static final int ORIS_ID = 4242;

    @BeforeEach
    void setUp() {
        adapter = new OrisEventSyncAdapter(eventManagementPort, orisEventDetailsReader, eventRepository);
    }

    private static OrisEventProjection projection(String name, LocalDate date, String location, String organizer,
                                                  EventTypeId resolvedEventTypeId) {
        return new OrisEventProjection(name, date, location, organizer, null,
                null, null, null, List.of(), null, null, null, null, null, resolvedEventTypeId, ORIS_ID);
    }

    @Test
    @DisplayName("declares pull-only-creating capabilities: no outward write, creates the local side, no sensitive data")
    void declaresPullOnlyCreatingCapabilities() {
        var capabilities = adapter.capabilities();

        assertThat(capabilities.readsLocal()).isTrue();
        assertThat(capabilities.readsExternal()).isTrue();
        assertThat(capabilities.writesLocal()).isTrue();
        assertThat(capabilities.writesExternal()).isFalse();
        assertThat(capabilities.createsLocal()).isTrue();
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
        @DisplayName("returns the projection read through OrisEventDetailsReader")
        void mapsOrisFieldsIntoProjection() {
            OrisEventProjection fields = projection("Spring Sprint", LocalDate.of(2026, 5, 1), "Brno Park", "OOB", null);
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(fields);

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
        @DisplayName("writes the projection inward via Event.syncFromOris and saves through EventRepository")
        void writesInwardViaSyncFromOris() {
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(projection("New name", LocalDate.of(2026, 5, 1), "Brno Park", "OOB", null));
            // The engine always calls readExternal before applyToLocal within one pass
            // (design.md "How a pass runs") and passes the very projection readExternal
            // returned — that is what now carries the resolved event type across
            // without a second ORIS read (task 8.10).
            SyncProjection incoming = adapter.readExternal(String.valueOf(ORIS_ID));

            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .name("Old name")
                    .eventDate(LocalDate.of(2026, 5, 1))
                    .location("Brno Park")
                    .organizer("OOB")
                    .build());
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            adapter.applyToLocal(EVENT_UUID.toString(), incoming);

            assertThat(event.getName()).isEqualTo("New name");
            verify(eventRepository).save(event);
        }

        @Test
        @DisplayName("throws EventNotFoundException when the event does not exist")
        void throwsWhenEventNotFound() {
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(projection("New name", LocalDate.of(2026, 5, 1), "Brno Park", "OOB", null));
            SyncProjection incoming = adapter.readExternal(String.valueOf(ORIS_ID));
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.applyToLocal(EVENT_UUID.toString(), incoming))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("carries the event type resolved by readExternal, without reading ORIS a second time (task 8.10)")
        void carriesResolvedEventTypeWithoutSecondOrisRead() {
            EventTypeId resolvedType = EventTypeId.generate();
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(projection("New name", LocalDate.of(2026, 5, 1), "Brno Park", "OOB", resolvedType));
            SyncProjection incoming = adapter.readExternal(String.valueOf(ORIS_ID));

            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .name("Old name")
                    .eventDate(LocalDate.of(2026, 5, 1))
                    .location("Brno Park")
                    .organizer("OOB")
                    .build());
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            adapter.applyToLocal(EVENT_UUID.toString(), incoming);

            assertThat(event.getEventTypeId()).contains(resolvedType);
            // read must have been called exactly once — by readExternal — not
            // again inside applyToLocal.
            verify(orisEventDetailsReader, Mockito.times(1)).read(ORIS_ID);
        }

        @Test
        @DisplayName("does not overwrite an existing eventTypeId during sync even when a different type was resolved (moved from OrisEventTypeAutoMappingTest.SyncAutoMapping)")
        void preservesExistingEventTypeOnSync() {
            EventTypeId existingTypeId = EventTypeId.generate();
            EventTypeId differentResolvedType = EventTypeId.generate();
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(projection("New name", LocalDate.of(2026, 5, 1), "Brno Park", "OOB", differentResolvedType));
            SyncProjection incoming = adapter.readExternal(String.valueOf(ORIS_ID));

            Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                    .withEventTypeId(existingTypeId)
                    .withName("Old Name")
                    .build();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            adapter.applyToLocal(EVENT_UUID.toString(), incoming);

            assertThat(event.getEventTypeId()).contains(existingTypeId);
        }

        @Test
        @DisplayName("never leaks one record's resolved event type onto a different record's write (regression for the removed ThreadLocal)")
        void doesNotLeakResolvedEventTypeBetweenRecords() {
            int otherOrisId = ORIS_ID + 1;
            EventId otherEventId = new EventId(UUID.randomUUID());
            EventTypeId firstResolvedType = EventTypeId.generate();

            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(projection("First event", LocalDate.of(2026, 5, 1), "Brno Park", "OOB", firstResolvedType));
            when(orisEventDetailsReader.read(otherOrisId)).thenReturn(projection("Second event", LocalDate.of(2026, 6, 1), "Praha Park", "POB", null));

            // Two records processed in sequence on the same thread, exactly as the
            // engine does (D16 — one record at a time): reading the second record's
            // external side must not carry the first record's resolved event type
            // forward to the second record's inward write.
            adapter.readExternal(String.valueOf(ORIS_ID));
            SyncProjection secondIncoming = adapter.readExternal(String.valueOf(otherOrisId));

            Event otherEvent = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .name("Second event")
                    .eventDate(LocalDate.of(2026, 6, 1))
                    .location("Praha Park")
                    .organizer("POB")
                    .build());
            when(eventRepository.findById(otherEventId)).thenReturn(Optional.of(otherEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            adapter.applyToLocal(otherEventId.value().toString(), secondIncoming);

            assertThat(otherEvent.getEventTypeId()).isEmpty();
        }

        @Test
        @DisplayName("fills eventTypeId when event has none and the resolved discipline matched (moved from OrisEventTypeAutoMappingTest.SyncAutoMapping)")
        void fillsEventTypeIdOnSyncWhenEmpty() {
            EventTypeId matchedType = EventTypeId.generate();
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(projection("Klasická závod", LocalDate.of(2026, 5, 1), "Brno Park", "OOB", matchedType));
            SyncProjection incoming = adapter.readExternal(String.valueOf(ORIS_ID));

            Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                    .withName("Old Name")
                    .build();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            adapter.applyToLocal(EVENT_UUID.toString(), incoming);

            assertThat(event.getEventTypeId()).contains(matchedType);
        }

        @Test
        @DisplayName("does not overwrite existing eventTypeId when discipline has no catalog match (moved from OrisEventTypeAutoMappingTest.SyncAutoMapping)")
        void preservesExistingEventTypeWhenNoDisciplineMatch() {
            EventTypeId existingTypeId = EventTypeId.generate();
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(projection("Strange Resync", LocalDate.of(2026, 5, 1), "Brno Park", "OOB", null));
            SyncProjection incoming = adapter.readExternal(String.valueOf(ORIS_ID));

            Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                    .withEventTypeId(existingTypeId)
                    .withName("Old Name")
                    .build();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            adapter.applyToLocal(EVENT_UUID.toString(), incoming);

            assertThat(event.getEventTypeId()).contains(existingTypeId);
        }

        @Test
        @DisplayName("preserves existing eventTypeId when discipline is null (moved from OrisEventTypeAutoMappingTest.SyncAutoMapping)")
        void preservesExistingEventTypeWhenDisciplineIsNull() {
            EventTypeId existingTypeId = EventTypeId.generate();
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(projection("No Discipline Resync", LocalDate.of(2026, 5, 1), "Brno Park", "OOB", null));
            SyncProjection incoming = adapter.readExternal(String.valueOf(ORIS_ID));

            Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                    .withEventTypeId(existingTypeId)
                    .withName("Old Name")
                    .build();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            adapter.applyToLocal(EVENT_UUID.toString(), incoming);

            assertThat(event.getEventTypeId()).contains(existingTypeId);
        }

        @Test
        @DisplayName("maps ORIS ranking level into EventRanking on sync (moved from OrisEventImportServiceTest.RankingMapping)")
        void mapsLevelToRankingOnSync() {
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(new OrisEventProjection(
                    "Updated Race", LocalDate.of(2026, 9, 1), "Forest", "OOB", null,
                    null, null, null, List.of(), 5, "ŽB", "Žebříček B", null, null, null, ORIS_ID));
            SyncProjection incoming = adapter.readExternal(String.valueOf(ORIS_ID));

            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .name("Old Name").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Location").organizer("OOB").build());
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            adapter.applyToLocal(EVENT_UUID.toString(), incoming);

            assertThat(event.getRanking()).isNotNull();
            assertThat(event.getRanking().levelId()).isEqualTo(5);
            assertThat(event.getRanking().shortName()).isEqualTo("ŽB");
        }

        @Test
        @DisplayName("sets ranking to null when the projection carries no ranking (moved from OrisEventImportServiceTest.RankingMapping)")
        void setsRankingNullWhenAbsentOnSync() {
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(projection("Updated Race", LocalDate.of(2026, 9, 1), "Forest", "OOB", null));
            SyncProjection incoming = adapter.readExternal(String.valueOf(ORIS_ID));

            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .name("Old Name").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Location").organizer("OOB").build());
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            adapter.applyToLocal(EVENT_UUID.toString(), incoming);

            assertThat(event.getRanking()).isNull();
        }

        @Test
        @DisplayName("derives baseEntryFee from the projection's amount and currency on sync (moved from OrisEventImportServiceTest.BaseEntryFeeMapping)")
        void derivesBaseEntryFeeOnSync() {
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(new OrisEventProjection(
                    "Sync Fee Race", LocalDate.of(2026, 9, 1), "Forest", "OOB", null,
                    null, null, null, List.of(), null, null, null, new java.math.BigDecimal("400"), "CZK", null, ORIS_ID));
            SyncProjection incoming = adapter.readExternal(String.valueOf(ORIS_ID));

            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .name("Old Name").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Location").organizer("OOB").build());
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            adapter.applyToLocal(EVENT_UUID.toString(), incoming);

            assertThat(event.getBaseEntryFee()).isNotNull();
            assertThat(event.getBaseEntryFee().amount()).isEqualByComparingTo(new java.math.BigDecimal("400"));
        }

        @Test
        @DisplayName("logs a warning when the sync removes categories that still have registrations (design.md D2)")
        void logsWarningWhenSyncRemovesCategoriesWithRegistrations() {
            EventCategory m21 = new EventCategory(EventCategoryId.generate(), "M21", "M21", null);
            EventCategory w21 = new EventCategory(EventCategoryId.generate(), "W21", "W21", null);
            LocalDate eventDate = LocalDate.now().plusDays(30);
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .name("Race")
                    .eventDate(eventDate)
                    .location("Forest")
                    .organizer("OOB")
                    .categories(List.of(m21, w21))
                    .build());
            event.publish();
            MemberId memberId = new MemberId(UUID.randomUUID());
            register(event, memberId, new SiCardNumber("12345"), m21.id());

            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(new OrisEventProjection(
                    "Race Updated", eventDate.plusDays(14), "Forest", "OOB", null,
                    null, null, null, List.of(new OrisEventProjection.Category("W21", "W21")),
                    null, null, null, null, null, null, ORIS_ID));
            SyncProjection incoming = adapter.readExternal(String.valueOf(ORIS_ID));

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            adapter.applyToLocal(EVENT_UUID.toString(), incoming);

            verify(eventRepository).save(event);
            assertThat(event.getCategories()).extracting(EventCategory::name)
                    .containsExactly("W21");
        }
    }

    @Nested
    @DisplayName("createLocal()")
    class CreateLocalMethod {

        @Test
        @DisplayName("builds an event from the ORIS projection, applies the auto-mapped event type, and returns its identifier")
        void buildsEventFromProjectionAndReturnsId() {
            EventTypeId resolvedType = EventTypeId.generate();
            when(orisEventDetailsReader.read(ORIS_ID)).thenReturn(projection("Spring Sprint", LocalDate.of(2026, 5, 1), "Brno Park", "OOB", resolvedType));
            SyncProjection externalProjection = adapter.readExternal(String.valueOf(ORIS_ID));

            ArgumentCaptor<Event> savedEventCaptor = ArgumentCaptor.forClass(Event.class);
            when(eventRepository.save(savedEventCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            String entityId = adapter.createLocal(externalProjection);

            Event saved = savedEventCaptor.getValue();
            assertThat(entityId).isEqualTo(saved.getId().value().toString());
            assertThat(saved.getName()).isEqualTo("Spring Sprint");
            assertThat(saved.getLocation()).isEqualTo("Brno Park");
            assertThat(saved.getOrganizer()).isEqualTo("OOB");
            assertThat(saved.getEventTypeId()).contains(resolvedType);
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
                    null, null, null, List.of(), null, null, null, null, null, null, 0);

            assertThatThrownBy(() -> adapter.applyToExternal("4242", projection))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
