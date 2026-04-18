package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.events.EventId;
import com.klabis.events.domain.*;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventImportService")
class OrisEventImportServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrisApiClient orisApiClient;

    @Mock
    private OrisWebUrls orisWebUrls;

    private OrisEventImportService service;

    @BeforeEach
    void setUp() {
        service = new OrisEventImportService(eventRepository, orisApiClient, orisWebUrls);
    }

    @Nested
    @DisplayName("importEventFromOris()")
    class ImportEventFromOrisMethod {

        @Test
        @DisplayName("should import event successfully from ORIS")
        void shouldImportEventSuccessfully() {
            int orisId = 9876;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetails(orisId, "Spring Sprint", LocalDate.of(2026, 8, 15), "Brno Park", org1, null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getOrisId()).isEqualTo(orisId);
            assertThat(result.getName()).isEqualTo("Spring Sprint");
            assertThat(result.getOrganizer()).isEqualTo("OOB");
            assertThat(result.getWebsiteUrl().value()).isEqualTo("https://oris.ceskyorientak.cz/Zavod?id=9876");
        }

        @Test
        @DisplayName("should throw DuplicateOrisImportException when DB unique constraint violated")
        void shouldThrowDuplicateExceptionOnConstraintViolation() {
            int orisId = 1111;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetails(orisId, "Duplicate Event", LocalDate.of(2026, 8, 15), "Location", org1, null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate oris_id"));

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isInstanceOf(DuplicateOrisImportException.class);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when ORIS returns no data for the given ID")
        void shouldThrowEventNotFoundWhenOrisReturnsEmpty() {
            int orisId = 9999;

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(null, "JSON", "OK", null, "getEvent"));

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("should use org2 abbreviation as organizer when org1 abbreviation is blank")
        void shouldFallBackToOrg2WhenOrg1AbbreviationIsBlank() {
            int orisId = 5555;
            Organizer org1 = new Organizer(1, "", "Empty Org");
            Organizer org2 = new Organizer(205, "PRG", "Prague OB");
            EventDetails details = buildEventDetails(orisId, "Prague Race", LocalDate.of(2026, 9, 1), "Prague", org1, org2);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getOrganizer()).isEqualTo("PRG");
        }

        @Test
        @DisplayName("should use UNKNOWN_ORGANIZER when both org1 and org2 abbreviations are blank")
        void shouldUseUnknownOrganizerWhenBothOrgsAreBlank() {
            int orisId = 7777;
            Organizer org1 = new Organizer(1, null, "Unnamed Org");
            EventDetails details = buildEventDetails(orisId, "Unnamed Race", LocalDate.of(2026, 10, 1), "Somewhere", org1, null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getOrganizer()).isEqualTo("---");
        }
    }

    @Nested
    @DisplayName("syncEventFromOris()")
    class SyncEventFromOrisMethod {

        @Test
        @DisplayName("should fetch event from ORIS and sync all fields")
        void shouldSyncEventFromOris() {
            EventId eventId = EventId.generate();
            int orisId = 9876;
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(orisId)
                    .name("Old Name")
                    .eventDate(LocalDate.of(2026, 8, 1))
                    .location("Old Location")
                    .organizer("OLD")
                    .build());

            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetails(orisId, "New Name from ORIS", LocalDate.of(2026, 8, 15), "New Location", org1, null);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            assertThat(event.getName()).isEqualTo("New Name from ORIS");
            assertThat(event.getLocation()).isEqualTo("New Location");
            assertThat(event.getOrganizer()).isEqualTo("OOB");
            verify(eventRepository).save(event);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void shouldThrowWhenEventNotFound() {
            EventId eventId = EventId.generate();
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.syncEventFromOris(eventId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("should log warning when sync removes categories that have existing registrations")
        void shouldLogWarningWhenSyncRemovesCategoriesWithRegistrations() {
            EventId eventId = EventId.generate();
            int orisId = 9876;
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(orisId)
                    .name("Race")
                    .eventDate(LocalDate.of(2026, 8, 1))
                    .location("Forest")
                    .organizer("OOB")
                    .categories(java.util.List.of("M21", "W21"))
                    .build());
            event.publish();
            MemberId memberId = new MemberId(UUID.randomUUID());
            event.registerMember(memberId, new SiCardNumber("12345"), "M21");

            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetailsWithClasses(orisId, "Race Updated", LocalDate.of(2026, 8, 15), "Forest",
                    org1, null, Map.of("W21", mockClass("W21")));

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            verify(eventRepository).save(event);
        }
    }

    private EventDetails buildEventDetails(int id, String name, LocalDate date, String place,
                                           Organizer org1, Organizer org2) {
        EventDetails details = Mockito.mock(EventDetails.class);
        Mockito.when(details.name()).thenReturn(name);
        Mockito.when(details.date()).thenReturn(date);
        Mockito.when(details.place()).thenReturn(place);
        Mockito.when(details.org1()).thenReturn(org1);
        Mockito.lenient().when(details.org2()).thenReturn(org2);
        Mockito.lenient().when(details.entryDate1()).thenReturn(null);
        Mockito.lenient().when(details.classes()).thenReturn(null);
        return details;
    }

    private EventDetails buildEventDetailsWithClasses(int id, String name, LocalDate date, String place,
                                                       Organizer org1, Organizer org2,
                                                       Map<String, com.dpolach.api.orisclient.dto.EventClass> classes) {
        EventDetails details = Mockito.mock(EventDetails.class);
        Mockito.when(details.name()).thenReturn(name);
        Mockito.when(details.date()).thenReturn(date);
        Mockito.when(details.place()).thenReturn(place);
        Mockito.when(details.org1()).thenReturn(org1);
        Mockito.lenient().when(details.org2()).thenReturn(org2);
        Mockito.lenient().when(details.entryDate1()).thenReturn(null);
        Mockito.when(details.classes()).thenReturn(classes);
        return details;
    }

    private com.dpolach.api.orisclient.dto.EventClass mockClass(String name) {
        com.dpolach.api.orisclient.dto.EventClass cls = Mockito.mock(com.dpolach.api.orisclient.dto.EventClass.class);
        Mockito.when(cls.name()).thenReturn(name);
        return cls;
    }
}
