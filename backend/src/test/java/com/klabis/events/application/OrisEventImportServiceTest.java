package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Level;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
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

    @Nested
    @DisplayName("RegistrationDeadlines mapping")
    class RegistrationDeadlinesMapping {

        @Test
        @DisplayName("should import event with single EntryDate1 → one deadline")
        void shouldImportSingleDeadline() {
            int orisId = 1001;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            LocalDate d1 = LocalDate.of(2026, 5, 15);
            EventDetails details = Mockito.mock(EventDetails.class);
            Mockito.when(details.name()).thenReturn("Race with one deadline");
            Mockito.when(details.date()).thenReturn(LocalDate.of(2026, 6, 1));
            Mockito.when(details.place()).thenReturn("Forest");
            Mockito.when(details.org1()).thenReturn(org1);
            Mockito.lenient().when(details.org2()).thenReturn(null);
            Mockito.when(details.entryDate1()).thenReturn(d1.atStartOfDay(java.time.ZoneId.of("Europe/Prague")));
            Mockito.when(details.entryDate2()).thenReturn(null);
            Mockito.when(details.entryDate3()).thenReturn(null);
            Mockito.lenient().when(details.classes()).thenReturn(null);
            Mockito.lenient().when(details.level()).thenReturn(null);
            Mockito.lenient().when(details.currency()).thenReturn(null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getRegistrationDeadlines().deadline1()).contains(d1);
            assertThat(result.getRegistrationDeadlines().deadline2()).isEmpty();
            assertThat(result.getRegistrationDeadlines().deadline3()).isEmpty();
        }

        @Test
        @DisplayName("should import event with EntryDate1+2+3 → three deadlines")
        void shouldImportThreeDeadlines() {
            int orisId = 1002;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            LocalDate d1 = LocalDate.of(2026, 4, 1);
            LocalDate d2 = LocalDate.of(2026, 5, 1);
            LocalDate d3 = LocalDate.of(2026, 6, 1);
            EventDetails details = Mockito.mock(EventDetails.class);
            Mockito.when(details.name()).thenReturn("Race with three deadlines");
            Mockito.when(details.date()).thenReturn(LocalDate.of(2026, 7, 1));
            Mockito.when(details.place()).thenReturn("Forest");
            Mockito.when(details.org1()).thenReturn(org1);
            Mockito.lenient().when(details.org2()).thenReturn(null);
            Mockito.when(details.entryDate1()).thenReturn(d1.atStartOfDay(java.time.ZoneId.of("Europe/Prague")));
            Mockito.when(details.entryDate2()).thenReturn(d2.atStartOfDay(java.time.ZoneId.of("Europe/Prague")));
            Mockito.when(details.entryDate3()).thenReturn(d3.atStartOfDay(java.time.ZoneId.of("Europe/Prague")));
            Mockito.lenient().when(details.classes()).thenReturn(null);
            Mockito.lenient().when(details.level()).thenReturn(null);
            Mockito.lenient().when(details.currency()).thenReturn(null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getRegistrationDeadlines().deadline1()).contains(d1);
            assertThat(result.getRegistrationDeadlines().deadline2()).contains(d2);
            assertThat(result.getRegistrationDeadlines().deadline3()).contains(d3);
        }

        @Test
        @DisplayName("should fail loudly when ORIS provides EntryDate1 and EntryDate3 but not EntryDate2")
        void shouldFailWhenDeadline1And3PresentButNot2() {
            int orisId = 1003;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            LocalDate d1 = LocalDate.of(2026, 4, 1);
            LocalDate d3 = LocalDate.of(2026, 6, 1);
            EventDetails details = Mockito.mock(EventDetails.class);
            Mockito.lenient().when(details.name()).thenReturn("Bad ORIS data");
            Mockito.lenient().when(details.date()).thenReturn(LocalDate.of(2026, 7, 1));
            Mockito.lenient().when(details.place()).thenReturn("Forest");
            Mockito.lenient().when(details.org1()).thenReturn(org1);
            Mockito.lenient().when(details.org2()).thenReturn(null);
            Mockito.when(details.entryDate1()).thenReturn(d1.atStartOfDay(java.time.ZoneId.of("Europe/Prague")));
            Mockito.when(details.entryDate2()).thenReturn(null);
            Mockito.when(details.entryDate3()).thenReturn(d3.atStartOfDay(java.time.ZoneId.of("Europe/Prague")));
            Mockito.lenient().when(details.classes()).thenReturn(null);
            Mockito.lenient().when(details.level()).thenReturn(null);
            Mockito.lenient().when(details.currency()).thenReturn(null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isInstanceOf(com.klabis.common.exceptions.BusinessRuleViolationException.class)
                    .hasMessageContaining("invalid registration deadlines");
        }
    }

    @Nested
    @DisplayName("Ranking mapping from ORIS Level")
    class RankingMapping {

        @Test
        @DisplayName("should map level to EventRanking on import")
        void shouldMapLevelToRankingOnImport() {
            int orisId = 2001;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Level level = new Level(3, "MČR", "Mistrovství ČR", "Czech Championships");
            EventDetails details = buildEventDetailsWithLevel(orisId, "MČR 2026", LocalDate.of(2026, 9, 1), "Forest", org1, level);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getRanking()).isNotNull();
            assertThat(result.getRanking().levelId()).isEqualTo(3);
            assertThat(result.getRanking().shortName()).isEqualTo("MČR");
            assertThat(result.getRanking().name()).isEqualTo("Mistrovství ČR");
        }

        @Test
        @DisplayName("should set ranking to null when level is null on import")
        void shouldSetRankingNullWhenLevelNullOnImport() {
            int orisId = 2002;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetailsWithLevel(orisId, "Local Race", LocalDate.of(2026, 9, 1), "Forest", org1, null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getRanking()).isNull();
        }

        @Test
        @DisplayName("should map level to EventRanking on sync")
        void shouldMapLevelToRankingOnSync() {
            EventId eventId = EventId.generate();
            int orisId = 2003;
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(orisId).name("Old Name").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Location").organizer("OOB").build());

            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Level level = new Level(5, "ŽB", "Žebříček B", "Ranking B");
            EventDetails details = buildEventDetailsWithLevel(orisId, "Updated Race", LocalDate.of(2026, 9, 1), "Forest", org1, level);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            assertThat(event.getRanking()).isNotNull();
            assertThat(event.getRanking().levelId()).isEqualTo(5);
            assertThat(event.getRanking().shortName()).isEqualTo("ŽB");
        }

        @Test
        @DisplayName("should set ranking to null when level is null on sync")
        void shouldSetRankingNullWhenLevelNullOnSync() {
            EventId eventId = EventId.generate();
            int orisId = 2004;
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(orisId).name("Old Name").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Location").organizer("OOB").build());

            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetailsWithLevel(orisId, "Updated Race", LocalDate.of(2026, 9, 1), "Forest", org1, null);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            assertThat(event.getRanking()).isNull();
        }
    }

    @Nested
    @DisplayName("BaseEntryFee derivation from MAX(EventClass.fee)")
    class BaseEntryFeeMapping {

        @Test
        @DisplayName("should derive baseEntryFee as MAX fee across classes")
        void shouldDeriveMaxFeeAcrossClasses() {
            int orisId = 3001;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Map<String, com.dpolach.api.orisclient.dto.EventClass> classes = Map.of(
                    "M21", mockClassWithFee("M21", "250"),
                    "W21", mockClassWithFee("W21", "200"),
                    "M35", mockClassWithFee("M35", "180")
            );
            EventDetails details = buildEventDetailsWithClassesAndCurrency(orisId, "Fee Race", LocalDate.of(2026, 9, 1),
                    "Forest", org1, classes, "CZK");

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getBaseEntryFee()).isNotNull();
            assertThat(result.getBaseEntryFee().amount()).isEqualByComparingTo(new BigDecimal("250"));
            assertThat(result.getBaseEntryFee().currency()).isEqualTo(Currency.getInstance("CZK"));
        }

        @Test
        @DisplayName("should ignore empty and unparseable fee values")
        void shouldIgnoreEmptyAndUnparseableFees() {
            int orisId = 3002;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Map<String, com.dpolach.api.orisclient.dto.EventClass> classes = Map.of(
                    "M21", mockClassWithFee("M21", "300"),
                    "W21", mockClassWithFee("W21", ""),
                    "M35", mockClassWithFee("M35", "N/A")
            );
            EventDetails details = buildEventDetailsWithClassesAndCurrency(orisId, "Partial Fee Race", LocalDate.of(2026, 9, 1),
                    "Forest", org1, classes, "CZK");

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getBaseEntryFee()).isNotNull();
            assertThat(result.getBaseEntryFee().amount()).isEqualByComparingTo(new BigDecimal("300"));
        }

        @Test
        @DisplayName("should return null baseEntryFee when all fees are empty or unparseable")
        void shouldReturnNullBaseEntryFeeWhenNoParseableFees() {
            int orisId = 3003;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Map<String, com.dpolach.api.orisclient.dto.EventClass> classes = Map.of(
                    "M21", mockClassWithFee("M21", ""),
                    "W21", mockClassWithFee("W21", "free")
            );
            EventDetails details = buildEventDetailsWithClassesAndCurrency(orisId, "No Fee Race", LocalDate.of(2026, 9, 1),
                    "Forest", org1, classes, "CZK");

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getBaseEntryFee()).isNull();
        }

        @Test
        @DisplayName("should return null baseEntryFee when classes are null")
        void shouldReturnNullBaseEntryFeeWhenClassesNull() {
            int orisId = 3004;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetails(orisId, "No Classes Race", LocalDate.of(2026, 9, 1), "Forest", org1, null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getBaseEntryFee()).isNull();
        }

        @Test
        @DisplayName("should use CZK as default currency when currency is blank")
        void shouldUseCzkAsDefaultWhenCurrencyIsBlank() {
            int orisId = 3005;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Map<String, com.dpolach.api.orisclient.dto.EventClass> classes = Map.of(
                    "M21", mockClassWithFee("M21", "150")
            );
            EventDetails details = buildEventDetailsWithClassesAndCurrency(orisId, "No Currency Race", LocalDate.of(2026, 9, 1),
                    "Forest", org1, classes, "");

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getBaseEntryFee()).isNotNull();
            assertThat(result.getBaseEntryFee().currency()).isEqualTo(Currency.getInstance("CZK"));
        }

        @Test
        @DisplayName("should use CZK as default currency when currency is invalid")
        void shouldUseCzkAsDefaultWhenCurrencyIsInvalid() {
            int orisId = 3006;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Map<String, com.dpolach.api.orisclient.dto.EventClass> classes = Map.of(
                    "M21", mockClassWithFee("M21", "150")
            );
            EventDetails details = buildEventDetailsWithClassesAndCurrency(orisId, "Invalid Currency Race", LocalDate.of(2026, 9, 1),
                    "Forest", org1, classes, "INVALID");

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getBaseEntryFee()).isNotNull();
            assertThat(result.getBaseEntryFee().currency()).isEqualTo(Currency.getInstance("CZK"));
        }

        @Test
        @DisplayName("should derive baseEntryFee on sync")
        void shouldDeriveBaseEntryFeeOnSync() {
            EventId eventId = EventId.generate();
            int orisId = 3007;
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(orisId).name("Old Name").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Location").organizer("OOB").build());

            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Map<String, com.dpolach.api.orisclient.dto.EventClass> classes = Map.of(
                    "M21", mockClassWithFee("M21", "400"),
                    "W21", mockClassWithFee("W21", "350")
            );
            EventDetails details = buildEventDetailsWithClassesAndCurrency(orisId, "Sync Fee Race", LocalDate.of(2026, 9, 1),
                    "Forest", org1, classes, "CZK");

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            service.syncEventFromOris(eventId);

            assertThat(event.getBaseEntryFee()).isNotNull();
            assertThat(event.getBaseEntryFee().amount()).isEqualByComparingTo(new BigDecimal("400"));
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
        Mockito.lenient().when(details.entryDate2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate3()).thenReturn(null);
        Mockito.lenient().when(details.classes()).thenReturn(null);
        Mockito.lenient().when(details.discipline()).thenReturn(null);
        Mockito.lenient().when(details.level()).thenReturn(null);
        Mockito.lenient().when(details.currency()).thenReturn(null);
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
        Mockito.lenient().when(details.entryDate2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate3()).thenReturn(null);
        Mockito.when(details.classes()).thenReturn(classes);
        Mockito.lenient().when(details.discipline()).thenReturn(null);
        Mockito.lenient().when(details.level()).thenReturn(null);
        Mockito.lenient().when(details.currency()).thenReturn(null);
        return details;
    }

    private com.dpolach.api.orisclient.dto.EventClass mockClass(String name) {
        com.dpolach.api.orisclient.dto.EventClass cls = Mockito.mock(com.dpolach.api.orisclient.dto.EventClass.class);
        Mockito.when(cls.name()).thenReturn(name);
        return cls;
    }

    private com.dpolach.api.orisclient.dto.EventClass mockClassWithFee(String name, String fee) {
        com.dpolach.api.orisclient.dto.EventClass cls = Mockito.mock(com.dpolach.api.orisclient.dto.EventClass.class);
        Mockito.when(cls.name()).thenReturn(name);
        Mockito.when(cls.fee()).thenReturn(fee);
        return cls;
    }

    private EventDetails buildEventDetailsWithLevel(int id, String name, LocalDate date, String place,
                                                     Organizer org1, Level level) {
        EventDetails details = Mockito.mock(EventDetails.class);
        Mockito.when(details.name()).thenReturn(name);
        Mockito.when(details.date()).thenReturn(date);
        Mockito.when(details.place()).thenReturn(place);
        Mockito.when(details.org1()).thenReturn(org1);
        Mockito.lenient().when(details.org2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate1()).thenReturn(null);
        Mockito.lenient().when(details.entryDate2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate3()).thenReturn(null);
        Mockito.lenient().when(details.classes()).thenReturn(null);
        Mockito.lenient().when(details.discipline()).thenReturn(null);
        Mockito.when(details.level()).thenReturn(level);
        Mockito.lenient().when(details.currency()).thenReturn(null);
        return details;
    }

    private EventDetails buildEventDetailsWithClassesAndCurrency(int id, String name, LocalDate date, String place,
                                                                   Organizer org1,
                                                                   Map<String, com.dpolach.api.orisclient.dto.EventClass> classes,
                                                                   String currency) {
        EventDetails details = Mockito.mock(EventDetails.class);
        Mockito.when(details.name()).thenReturn(name);
        Mockito.when(details.date()).thenReturn(date);
        Mockito.when(details.place()).thenReturn(place);
        Mockito.when(details.org1()).thenReturn(org1);
        Mockito.lenient().when(details.org2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate1()).thenReturn(null);
        Mockito.lenient().when(details.entryDate2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate3()).thenReturn(null);
        Mockito.when(details.classes()).thenReturn(classes);
        Mockito.lenient().when(details.discipline()).thenReturn(null);
        Mockito.lenient().when(details.level()).thenReturn(null);
        Mockito.when(details.currency()).thenReturn(currency);
        return details;
    }
}
