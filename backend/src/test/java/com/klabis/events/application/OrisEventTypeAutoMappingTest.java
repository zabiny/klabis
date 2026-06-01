package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ORIS event type auto-mapping via Discipline ID")
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
        @DisplayName("should set eventTypeId when discipline ID has a catalog match")
        void shouldSetEventTypeIdWhenDisciplineMatches() {
            int orisId = 100;
            Discipline discipline = new Discipline(3, "SP", "Sprint", "Sprint");
            EventType matchedType = EventType.create(
                    new EventType.CreateEventType("Sprint", null, 1, java.util.Set.of(3)), 1);

            EventDetails details = buildDetailsWithDiscipline(orisId, "Sprint závod", discipline);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByOrisDisciplineId(3)).thenReturn(Optional.of(matchedType));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getEventTypeId()).isPresent();
            assertThat(result.getEventTypeId().get()).isEqualTo(matchedType.getId());
        }

        @Test
        @DisplayName("should leave eventTypeId empty when discipline ID has no catalog match")
        void shouldLeaveEventTypeEmptyWhenNoDisciplineMatch() {
            int orisId = 101;
            Discipline discipline = new Discipline(99, "X", "Neznámá disciplína", "Unknown Discipline");

            EventDetails details = buildDetailsWithDiscipline(orisId, "Strange Race", discipline);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByOrisDisciplineId(99)).thenReturn(Optional.empty());
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getEventTypeId()).isEmpty();
        }

        @Test
        @DisplayName("should leave eventTypeId empty when ORIS discipline is null")
        void shouldLeaveEventTypeEmptyWhenDisciplineIsNull() {
            int orisId = 102;
            EventDetails details = buildDetailsWithDiscipline(orisId, "No Discipline Race", null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getEventTypeId()).isEmpty();
            Mockito.verify(eventTypeRepository, Mockito.never()).findByOrisDisciplineId(any(Integer.class));
        }

        @Test
        @DisplayName("should leave eventTypeId empty when ORIS discipline ID is 0 (missing/invalid)")
        void shouldLeaveEventTypeEmptyWhenDisciplineIdIsZero() {
            int orisId = 103;
            Discipline discipline = new Discipline(0, "", "", "");

            EventDetails details = buildDetailsWithDiscipline(orisId, "No Discipline ID Race", discipline);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getEventTypeId()).isEmpty();
            Mockito.verify(eventTypeRepository, Mockito.never()).findByOrisDisciplineId(any(Integer.class));
        }
    }

    @Nested
    @DisplayName("syncEventFromOris() — auto-mapping with preserve behavior")
    class SyncAutoMapping {

        @Test
        @DisplayName("should fill eventTypeId when event has none and discipline ID matches")
        void shouldFillEventTypeIdOnSyncWhenEmpty() {
            EventId eventId = EventId.generate();
            int orisId = 200;
            Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                    .withOrisId(orisId)
                    .withName("Old Name")
                    .build();

            Discipline discipline = new Discipline(1, "CL", "Klasická trať", "Classic");
            EventType matchedType = EventType.create(
                    new EventType.CreateEventType("Klasika", null, 2, java.util.Set.of(1)), 2);

            EventDetails details = buildDetailsWithDiscipline(orisId, "Klasická závod", discipline);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByOrisDisciplineId(1)).thenReturn(Optional.of(matchedType));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            assertThat(event.getEventTypeId()).isPresent();
            assertThat(event.getEventTypeId().get()).isEqualTo(matchedType.getId());
        }

        @Test
        @DisplayName("should NOT overwrite existing eventTypeId during sync even when discipline matches")
        void shouldNotOverwriteExistingEventTypeOnSync() {
            EventId eventId = EventId.generate();
            int orisId = 201;
            EventTypeId existingTypeId = EventTypeId.generate();
            Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                    .withOrisId(orisId)
                    .withEventTypeId(existingTypeId)
                    .withName("Old Name")
                    .build();

            Discipline discipline = new Discipline(2, "KR", "Krátká trať", "Short");
            EventType differentType = EventType.create(
                    new EventType.CreateEventType("Krátká", null, 3, java.util.Set.of(2)), 3);

            EventDetails details = buildDetailsWithDiscipline(orisId, "Short Race Resync", discipline);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByOrisDisciplineId(2)).thenReturn(Optional.of(differentType));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            assertThat(event.getEventTypeId()).contains(existingTypeId);
        }

        @Test
        @DisplayName("should NOT overwrite existing eventTypeId during sync when discipline has no match")
        void shouldPreserveExistingEventTypeWhenNoDisciplineMatchDuringSync() {
            EventId eventId = EventId.generate();
            int orisId = 202;
            EventTypeId existingTypeId = EventTypeId.generate();
            Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                    .withOrisId(orisId)
                    .withEventTypeId(existingTypeId)
                    .withName("Old Name")
                    .build();

            Discipline discipline = new Discipline(99, "X", "Neznámá disciplína", "Unknown");
            EventDetails details = buildDetailsWithDiscipline(orisId, "Strange Resync", discipline);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByOrisDisciplineId(99)).thenReturn(Optional.empty());
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            assertThat(event.getEventTypeId()).contains(existingTypeId);
        }

        @Test
        @DisplayName("should preserve existing eventTypeId when discipline is null during sync")
        void shouldPreserveExistingEventTypeWhenDisciplineIsNullDuringSync() {
            EventId eventId = EventId.generate();
            int orisId = 203;
            EventTypeId existingTypeId = EventTypeId.generate();
            Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                    .withOrisId(orisId)
                    .withEventTypeId(existingTypeId)
                    .withName("Old Name")
                    .build();

            EventDetails details = buildDetailsWithDiscipline(orisId, "No Discipline Resync", null);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            assertThat(event.getEventTypeId()).contains(existingTypeId);
            Mockito.verify(eventTypeRepository, Mockito.never()).findByOrisDisciplineId(any(Integer.class));
        }
    }

    private EventDetails buildDetailsWithDiscipline(int id, String name, Discipline discipline) {
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
        Mockito.when(details.discipline()).thenReturn(discipline);
        return details;
    }

    private OrisApiClient.OrisResponse<EventDetails> okResponse(EventDetails details) {
        return new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent");
    }
}
