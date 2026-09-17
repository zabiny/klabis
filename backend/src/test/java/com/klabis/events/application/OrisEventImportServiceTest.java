package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Level;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.events.EventId;
import com.klabis.events.domain.*;
import com.klabis.sync.application.SynchronizationPort;
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

    @Mock
    private SynchronizationPort synchronizationPort;

    private OrisEventImportService service;

    @BeforeEach
    void setUp() {
        // The real reader stays wired in: importEventFromOris reads the ORIS fields
        // through it, so the import tests exercise the whole pipeline as production
        // does. The sync ("applyOrisSync") half of this behaviour now lives on
        // OrisEventSyncAdapter (events.infrastructure.orissync) after the D2 fold —
        // see OrisEventSyncAdapterTest for its inward-write coverage.
        OrisEventFieldsReader reader = new OrisEventFieldsReader(orisApiClient, orisWebUrls, eventTypeRepository);
        service = new OrisEventImportService(eventRepository, reader, synchronizationPort);
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
        @DisplayName("should enrol the imported event with the synchronisation engine (task 8.1)")
        void shouldEnrolImportedEventWithSynchronizationEngine() {
            int orisId = 9877;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetails(orisId, "Spring Sprint", LocalDate.of(2026, 8, 15), "Brno Park", org1, null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            verify(synchronizationPort).enroll(
                    new com.klabis.sync.domain.SyncTarget(com.klabis.sync.domain.SyncEntityType.EVENT, result.getId().value().toString()),
                    new com.klabis.sync.domain.ExternalReference(com.klabis.sync.domain.ExternalSystem.ORIS, String.valueOf(orisId)));
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

        @Test
        @DisplayName("should populate category orisId from ORIS EventClass.id() on import")
        void shouldPopulateCategoryOrisIdOnImport() {
            int orisId = 2222;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetailsWithClasses(orisId, "Race", LocalDate.of(2026, 8, 15), "Forest",
                    org1, null, Map.of("100", mockClass("100", "M21")));

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = service.importEventFromOris(orisId);

            assertThat(result.getCategories()).hasSize(1);
            assertThat(result.getCategories().get(0).orisId()).isEqualTo("100");
            assertThat(result.getCategories().get(0).name()).isEqualTo("M21");
        }
    }

    @Nested
    @DisplayName("syncEventFromOris()")
    class SyncEventFromOrisMethod {

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void shouldThrowWhenEventNotFound() {
            EventId eventId = EventId.generate();
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.syncEventFromOris(eventId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event is not enrolled for synchronisation")
        void shouldThrowWhenEventNotEnrolled() {
            EventId eventId = EventId.generate();
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(9876).name("Race").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Forest").organizer("OOB").build());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(synchronizationPort.findByTarget(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.syncEventFromOris(eventId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("should delegate to the synchronisation engine when the record is enrolled and not stuck (task 8.3)")
        void shouldDelegateToSynchronizationEngine() {
            EventId eventId = EventId.generate();
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(9876).name("Race").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Forest").organizer("OOB").build());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            com.klabis.sync.domain.SyncTarget target = new com.klabis.sync.domain.SyncTarget(
                    com.klabis.sync.domain.SyncEntityType.EVENT, eventId.value().toString());
            com.klabis.sync.domain.SyncRecord record = com.klabis.sync.domain.SyncRecord.enroll(
                    com.klabis.sync.SyncRecordId.newId(), target,
                    new com.klabis.sync.domain.ExternalReference(com.klabis.sync.domain.ExternalSystem.ORIS, "9876"));
            when(synchronizationPort.findByTarget(target)).thenReturn(Optional.of(record));

            service.syncEventFromOris(eventId);

            verify(synchronizationPort).synchronizeNow(record.getId(), null);
        }

        @Test
        @DisplayName("should refuse with EventSyncNeedsResolutionException when the record is in conflict")
        void shouldRefuseWhenRecordInConflict() {
            EventId eventId = EventId.generate();
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(9876).name("Race").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Forest").organizer("OOB").build());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            com.klabis.sync.domain.SyncTarget target = new com.klabis.sync.domain.SyncTarget(
                    com.klabis.sync.domain.SyncEntityType.EVENT, eventId.value().toString());
            com.klabis.sync.domain.SyncRecord record = Mockito.mock(com.klabis.sync.domain.SyncRecord.class);
            when(record.getStatus()).thenReturn(com.klabis.sync.domain.SyncStatus.CONFLICT);
            when(synchronizationPort.findByTarget(target)).thenReturn(Optional.of(record));

            assertThatThrownBy(() -> service.syncEventFromOris(eventId))
                    .isInstanceOf(EventSyncNeedsResolutionException.class);
        }
    }

    // applyOrisSync() — the engine's inward write — moved to OrisEventSyncAdapterTest
    // (events.infrastructure.orissync): field application, EventNotFoundException,
    // the category-removal warning (task 8.11) and category orisId population on sync
    // all now exercise OrisEventSyncAdapter.applyToLocal, where this behaviour lives
    // after the D2 fold.

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

        // "on sync" ranking mapping (mapping EventRanking / null on applyOrisSync)
        // moved to OrisEventSyncAdapterTest (events.infrastructure.orissync) —
        // OrisEventSyncAdapter.applyToLocal now reassembles EventRanking from the
        // projection directly (design.md D2, D4/task 5.4).
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

        // "should derive baseEntryFee on sync" moved to OrisEventSyncAdapterTest
        // (events.infrastructure.orissync) — OrisEventSyncAdapter.applyToLocal now
        // reassembles Money from the projection directly (design.md D2, D4/task 5.4).
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
        return mockClass(name, name);
    }

    private com.dpolach.api.orisclient.dto.EventClass mockClass(String orisClassId, String name) {
        com.dpolach.api.orisclient.dto.EventClass cls = Mockito.mock(com.dpolach.api.orisclient.dto.EventClass.class);
        Mockito.lenient().when(cls.id()).thenReturn(orisClassId);
        Mockito.when(cls.name()).thenReturn(name);
        return cls;
    }

    private com.dpolach.api.orisclient.dto.EventClass mockClassWithFee(String name, String fee) {
        com.dpolach.api.orisclient.dto.EventClass cls = Mockito.mock(com.dpolach.api.orisclient.dto.EventClass.class);
        Mockito.lenient().when(cls.id()).thenReturn(name);
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
