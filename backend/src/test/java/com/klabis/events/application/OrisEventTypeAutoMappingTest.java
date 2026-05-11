package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Level;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.eventtype.domain.EventType;
import com.klabis.events.eventtype.domain.EventTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * B5.4 — ORIS import auto-mapping integration tests.
 *
 * Verifies that Level.nameCZ from the ORIS payload is used for case-insensitive lookup
 * against the EventType catalog, and that missing/unmatched levels are handled gracefully.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ORIS event type auto-mapping")
class OrisEventTypeAutoMappingTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private OrisApiClient orisApiClient;

    @Mock
    private OrisWebUrls orisWebUrls;

    private OrisEventImportService service;

    @BeforeEach
    void setUp() {
        service = new OrisEventImportService(eventRepository, orisApiClient, orisWebUrls, eventTypeRepository);
    }

    @Nested
    @DisplayName("importEventFromOris() — auto-mapping")
    class ImportAutoMapping {

        @Test
        @DisplayName("should set eventTypeId when ORIS Level.nameCZ matches a catalog entry")
        void shouldSetEventTypeIdWhenLevelMatches() {
            int orisId = 100;
            EventTypeId expectedTypeId = EventTypeId.generate();
            EventType matchedType = EventType.create(
                    new EventType.CreateEventType("Trénink", null, 1), 1);
            Level level = new Level(5, "T", "Trénink", "Training");

            EventDetails details = buildDetailsWithLevel(orisId, "Noční trénink", level);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByNameIgnoreCase("Trénink")).thenReturn(Optional.of(matchedType));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getEventTypeId()).isPresent();
            assertThat(result.getEventTypeId().get()).isEqualTo(matchedType.getId());
        }

        @Test
        @DisplayName("should leave eventTypeId empty when ORIS Level has no matching catalog entry")
        void shouldLeaveEventTypeEmptyWhenNoMatch() {
            int orisId = 101;
            Level level = new Level(99, "X", "Neznámá úroveň", "Unknown Level");

            EventDetails details = buildDetailsWithLevel(orisId, "Strange Race", level);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByNameIgnoreCase("Neznámá úroveň")).thenReturn(Optional.empty());
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getEventTypeId()).isEmpty();
        }

        @Test
        @DisplayName("should leave eventTypeId empty when ORIS Level is null")
        void shouldLeaveEventTypeEmptyWhenLevelIsNull() {
            int orisId = 102;
            EventDetails details = buildDetailsWithLevel(orisId, "No Level Race", null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getEventTypeId()).isEmpty();
            Mockito.verify(eventTypeRepository, Mockito.never()).findByNameIgnoreCase(any());
        }

        @Test
        @DisplayName("should match catalog entry case-insensitively (ORIS 'TRÉNINK', catalog 'Trénink')")
        void shouldMatchCaseInsensitively() {
            int orisId = 103;
            EventType matchedType = EventType.create(
                    new EventType.CreateEventType("Trénink", null, 1), 1);
            Level level = new Level(5, "T", "TRÉNINK", "Training");

            EventDetails details = buildDetailsWithLevel(orisId, "Trénink v lese", level);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByNameIgnoreCase("TRÉNINK")).thenReturn(Optional.of(matchedType));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getEventTypeId()).isPresent();
            assertThat(result.getEventTypeId().get()).isEqualTo(matchedType.getId());
        }
    }

    @Nested
    @DisplayName("syncEventFromOris() — auto-mapping with preserve behavior")
    class SyncAutoMapping {

        @Test
        @DisplayName("should set eventTypeId when ORIS Level matches a catalog entry during sync")
        void shouldSetEventTypeIdOnSyncWhenLevelMatches() {
            EventId eventId = EventId.generate();
            int orisId = 200;
            Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                    .withOrisId(orisId)
                    .withName("Old Name")
                    .build();

            EventType matchedType = EventType.create(
                    new EventType.CreateEventType("Závod", null, 2), 2);
            Level level = new Level(3, "Z", "Závod", "Race");

            EventDetails details = buildDetailsWithLevel(orisId, "Závod v lese", level);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByNameIgnoreCase("Závod")).thenReturn(Optional.of(matchedType));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            assertThat(event.getEventTypeId()).isPresent();
            assertThat(event.getEventTypeId().get()).isEqualTo(matchedType.getId());
        }

        @Test
        @DisplayName("should preserve existing eventTypeId when ORIS Level has no catalog match during sync")
        void shouldPreserveExistingEventTypeWhenNoMatchDuringSync() {
            EventId eventId = EventId.generate();
            int orisId = 201;
            EventTypeId existingTypeId = EventTypeId.generate();
            Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                    .withOrisId(orisId)
                    .withEventTypeId(existingTypeId)
                    .withName("Old Name")
                    .build();

            Level level = new Level(99, "X", "Neznámá úroveň", "Unknown Level");
            EventDetails details = buildDetailsWithLevel(orisId, "Strange Resync", level);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByNameIgnoreCase("Neznámá úroveň")).thenReturn(Optional.empty());
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            assertThat(event.getEventTypeId()).contains(existingTypeId);
        }

        @Test
        @DisplayName("should preserve existing eventTypeId when ORIS Level is null during sync")
        void shouldPreserveExistingEventTypeWhenLevelIsNullDuringSync() {
            EventId eventId = EventId.generate();
            int orisId = 202;
            EventTypeId existingTypeId = EventTypeId.generate();
            Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                    .withOrisId(orisId)
                    .withEventTypeId(existingTypeId)
                    .withName("Old Name")
                    .build();

            EventDetails details = buildDetailsWithLevel(orisId, "No Level Resync", null);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            assertThat(event.getEventTypeId()).contains(existingTypeId);
            Mockito.verify(eventTypeRepository, Mockito.never()).findByNameIgnoreCase(any());
        }
    }

    private EventDetails buildDetailsWithLevel(int id, String name, Level level) {
        EventDetails details = Mockito.mock(EventDetails.class);
        Mockito.when(details.name()).thenReturn(name);
        Mockito.when(details.date()).thenReturn(LocalDate.of(2026, 9, 1));
        Mockito.when(details.place()).thenReturn("Brno");
        Mockito.when(details.org1()).thenReturn(new Organizer(1, "OOB", "Orel Brno"));
        Mockito.lenient().when(details.org2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate1()).thenReturn(null);
        Mockito.lenient().when(details.entryDate2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate3()).thenReturn(null);
        Mockito.lenient().when(details.classes()).thenReturn(null);
        Mockito.when(details.level()).thenReturn(level);
        return details;
    }

    private OrisApiClient.OrisResponse<EventDetails> okResponse(EventDetails details) {
        return new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent");
    }
}
