package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeRepository;
import com.klabis.sync.application.SynchronizationPort;
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

    @Mock
    private SynchronizationPort synchronizationPort;

    private OrisEventImportService service;

    @BeforeEach
    void setUp() {
        // The real reader stays wired in: importEventFromOris resolves the event type
        // through it, so the import tests exercise the whole pipeline as production
        // does. The sync ("applyOrisSync") half of this behaviour now lives on
        // OrisEventSyncAdapter (events.infrastructure.orissync) after the D2 fold —
        // see OrisEventSyncAdapterTest for its auto-mapping preserve/fill coverage.
        OrisEventFieldsReader reader = new OrisEventFieldsReader(orisApiClient, orisWebUrls, eventTypeRepository);
        service = new OrisEventImportService(eventRepository, reader, synchronizationPort);
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

    // applyOrisSync() auto-mapping with preserve behaviour (fill-when-empty,
    // preserve-when-set, across matched/unmatched/null discipline) moved to
    // OrisEventSyncAdapterTest (events.infrastructure.orissync) — that is where
    // applyToLocal (formerly applyOrisSync) now lives after the D2 fold.

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
