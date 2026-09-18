package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Organizer;
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

/**
 * Event-type auto-mapping (discipline ID → catalog) coverage, relocated from
 * {@code OrisEventTypeAutoMappingTest}'s "importEventFromOris() — auto-mapping" nested
 * class (task 9.1/9.2): {@code OrisEventImportService.importEventFromOris} no longer
 * calls {@link OrisEventFieldsReader} directly — the read now happens inside {@code
 * OrisEventSyncAdapter.readExternal} via the synchronisation engine's {@code
 * pullAndEnroll} (design.md D2, D3) — so this resolution is exercised directly against
 * {@link OrisEventFieldsReader#readOrisFields}, the one place it lives regardless of
 * which caller triggers the read.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventFieldsReader — event type auto-mapping via Discipline ID")
class OrisEventFieldsReaderTest {

    @Mock
    private OrisApiClient orisApiClient;

    @Mock
    private OrisWebUrls orisWebUrls;

    @Mock
    private EventTypeRepository eventTypeRepository;

    private OrisEventFieldsReader reader;

    @BeforeEach
    void setUp() {
        reader = new OrisEventFieldsReader(orisApiClient, orisWebUrls, eventTypeRepository);
    }

    @Nested
    @DisplayName("readOrisFields() — auto-mapping")
    class AutoMapping {

        @Test
        @DisplayName("should resolve eventTypeId when discipline ID has a catalog match")
        void shouldResolveEventTypeIdWhenDisciplineMatches() {
            int orisId = 100;
            Discipline discipline = new Discipline(3, "SP", "Sprint", "Sprint");
            EventType matchedType = EventType.create(
                    new EventType.CreateEventType("Sprint", null, 1, java.util.Set.of(3)), 1);

            EventDetails details = buildDetailsWithDiscipline("Sprint závod", discipline);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByOrisDisciplineId(3)).thenReturn(Optional.of(matchedType));

            OrisEventFields result = reader.readOrisFields(orisId);

            assertThat(result.resolvedEventTypeId()).isEqualTo(matchedType.getId());
        }

        @Test
        @DisplayName("should resolve no eventTypeId when discipline ID has no catalog match")
        void shouldResolveNoEventTypeWhenNoDisciplineMatch() {
            int orisId = 101;
            Discipline discipline = new Discipline(99, "X", "Neznámá disciplína", "Unknown Discipline");

            EventDetails details = buildDetailsWithDiscipline("Strange Race", discipline);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);
            when(eventTypeRepository.findByOrisDisciplineId(99)).thenReturn(Optional.empty());

            OrisEventFields result = reader.readOrisFields(orisId);

            assertThat(result.resolvedEventTypeId()).isNull();
        }

        @Test
        @DisplayName("should resolve no eventTypeId when ORIS discipline is null")
        void shouldResolveNoEventTypeWhenDisciplineIsNull() {
            int orisId = 102;
            EventDetails details = buildDetailsWithDiscipline("No Discipline Race", null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);

            OrisEventFields result = reader.readOrisFields(orisId);

            assertThat(result.resolvedEventTypeId()).isNull();
            Mockito.verify(eventTypeRepository, Mockito.never()).findByOrisDisciplineId(any(Integer.class));
        }

        @Test
        @DisplayName("should resolve no eventTypeId when ORIS discipline ID is 0 (missing/invalid)")
        void shouldResolveNoEventTypeWhenDisciplineIdIsZero() {
            int orisId = 103;
            Discipline discipline = new Discipline(0, "", "", "");

            EventDetails details = buildDetailsWithDiscipline("No Discipline ID Race", discipline);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(okResponse(details));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.example.cz/event/" + orisId);

            OrisEventFields result = reader.readOrisFields(orisId);

            assertThat(result.resolvedEventTypeId()).isNull();
            Mockito.verify(eventTypeRepository, Mockito.never()).findByOrisDisciplineId(any(Integer.class));
        }
    }

    @Test
    @DisplayName("should throw EventNotFoundException when ORIS returns no data for the given ID")
    void shouldThrowEventNotFoundWhenOrisReturnsEmpty() {
        int orisId = 9999;

        when(orisApiClient.getEventDetails(orisId)).thenReturn(
                new OrisApiClient.OrisResponse<>(null, "JSON", "OK", null, "getEvent"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> reader.readOrisFields(orisId))
                .isInstanceOf(EventNotFoundException.class);
    }

    private EventDetails buildDetailsWithDiscipline(String name, Discipline discipline) {
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
