package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventClass;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Level;
import com.dpolach.api.orisclient.dto.Organizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Field-mapping coverage for {@link OrisEventDetailsMapper}, relocated from
 * {@code OrisEventImportServiceTest} (task 9.1/9.2): {@code importEventFromOris} no
 * longer builds the {@link com.klabis.events.domain.Event} itself — that moved to
 * {@code OrisEventSyncAdapter.createLocal} (design.md D2) — so this mapping is
 * exercised directly against {@link OrisEventDetailsMapper#map} rather than through
 * the import service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventDetailsMapper")
class OrisEventDetailsMapperTest {

    @Mock
    private OrisWebUrls orisWebUrls;

    @Nested
    @DisplayName("organizer resolution")
    class OrganizerResolution {

        @Test
        @DisplayName("should use org2 abbreviation as organizer when org1 abbreviation is blank")
        void shouldFallBackToOrg2WhenOrg1AbbreviationIsBlank() {
            int orisId = 5555;
            Organizer org1 = new Organizer(1, "", "Empty Org");
            Organizer org2 = new Organizer(205, "PRG", "Prague OB");
            EventDetails details = buildEventDetails(orisId, "Prague Race", LocalDate.of(2026, 9, 1), "Prague", org1, org2);
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.organizer()).isEqualTo("PRG");
        }

        @Test
        @DisplayName("should use UNKNOWN_ORGANIZER when both org1 and org2 abbreviations are blank")
        void shouldUseUnknownOrganizerWhenBothOrgsAreBlank() {
            int orisId = 7777;
            Organizer org1 = new Organizer(1, null, "Unnamed Org");
            EventDetails details = buildEventDetails(orisId, "Unnamed Race", LocalDate.of(2026, 10, 1), "Somewhere", org1, null);
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.organizer()).isEqualTo("---");
        }
    }

    @Test
    @DisplayName("should populate category orisId from ORIS EventClass.id()")
    void shouldPopulateCategoryOrisId() {
        int orisId = 2222;
        Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
        EventDetails details = buildEventDetailsWithClasses(orisId, "Race", LocalDate.of(2026, 8, 15), "Forest",
                org1, null, Map.of("100", mockClass("100", "M21")));
        when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

        OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

        assertThat(result.categories()).hasSize(1);
        assertThat(result.categories().get(0).orisId()).isEqualTo("100");
        assertThat(result.categories().get(0).name()).isEqualTo("M21");
    }

    @Nested
    @DisplayName("RegistrationDeadlines mapping")
    class RegistrationDeadlinesMapping {

        @Test
        @DisplayName("single EntryDate1 → one deadline")
        void shouldMapSingleDeadline() {
            int orisId = 1001;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            LocalDate d1 = LocalDate.of(2026, 5, 15);
            EventDetails details = Mockito.mock(EventDetails.class);
            Mockito.when(details.name()).thenReturn("Race with one deadline");
            Mockito.when(details.date()).thenReturn(LocalDate.of(2026, 6, 1));
            Mockito.when(details.place()).thenReturn("Forest");
            Mockito.when(details.org1()).thenReturn(org1);
            Mockito.lenient().when(details.org2()).thenReturn(null);
            Mockito.when(details.entryDate1()).thenReturn(d1.atStartOfDay(ZoneId.of("Europe/Prague")));
            Mockito.when(details.entryDate2()).thenReturn(null);
            Mockito.when(details.entryDate3()).thenReturn(null);
            Mockito.lenient().when(details.classes()).thenReturn(null);
            Mockito.lenient().when(details.level()).thenReturn(null);
            Mockito.lenient().when(details.currency()).thenReturn(null);
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.registrationDeadlines().deadline1()).contains(d1);
            assertThat(result.registrationDeadlines().deadline2()).isEmpty();
            assertThat(result.registrationDeadlines().deadline3()).isEmpty();
        }

        @Test
        @DisplayName("EntryDate1+2+3 → three deadlines")
        void shouldMapThreeDeadlines() {
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
            Mockito.when(details.entryDate1()).thenReturn(d1.atStartOfDay(ZoneId.of("Europe/Prague")));
            Mockito.when(details.entryDate2()).thenReturn(d2.atStartOfDay(ZoneId.of("Europe/Prague")));
            Mockito.when(details.entryDate3()).thenReturn(d3.atStartOfDay(ZoneId.of("Europe/Prague")));
            Mockito.lenient().when(details.classes()).thenReturn(null);
            Mockito.lenient().when(details.level()).thenReturn(null);
            Mockito.lenient().when(details.currency()).thenReturn(null);
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.registrationDeadlines().deadline1()).contains(d1);
            assertThat(result.registrationDeadlines().deadline2()).contains(d2);
            assertThat(result.registrationDeadlines().deadline3()).contains(d3);
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
            Mockito.when(details.entryDate1()).thenReturn(d1.atStartOfDay(ZoneId.of("Europe/Prague")));
            Mockito.when(details.entryDate2()).thenReturn(null);
            Mockito.when(details.entryDate3()).thenReturn(d3.atStartOfDay(ZoneId.of("Europe/Prague")));
            Mockito.lenient().when(details.classes()).thenReturn(null);
            Mockito.lenient().when(details.level()).thenReturn(null);
            Mockito.lenient().when(details.currency()).thenReturn(null);
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            assertThatThrownBy(() -> OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null))
                    .isInstanceOf(com.klabis.common.exceptions.BusinessRuleViolationException.class)
                    .hasMessageContaining("invalid registration deadlines");
        }
    }

    @Nested
    @DisplayName("Ranking mapping from ORIS Level")
    class RankingMapping {

        @Test
        @DisplayName("should map level to EventRanking")
        void shouldMapLevelToRanking() {
            int orisId = 2001;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Level level = new Level(3, "MČR", "Mistrovství ČR", "Czech Championships");
            EventDetails details = buildEventDetailsWithLevel(orisId, "MČR 2026", LocalDate.of(2026, 9, 1), "Forest", org1, level);
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.ranking()).isNotNull();
            assertThat(result.ranking().levelId()).isEqualTo(3);
            assertThat(result.ranking().shortName()).isEqualTo("MČR");
            assertThat(result.ranking().name()).isEqualTo("Mistrovství ČR");
        }

        @Test
        @DisplayName("should set ranking to null when level is null")
        void shouldSetRankingNullWhenLevelNull() {
            int orisId = 2002;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetailsWithLevel(orisId, "Local Race", LocalDate.of(2026, 9, 1), "Forest", org1, null);
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.ranking()).isNull();
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
            Map<String, EventClass> classes = Map.of(
                    "M21", mockClassWithFee("M21", "250"),
                    "W21", mockClassWithFee("W21", "200"),
                    "M35", mockClassWithFee("M35", "180")
            );
            EventDetails details = buildEventDetailsWithClassesAndCurrency(orisId, "Fee Race", LocalDate.of(2026, 9, 1),
                    "Forest", org1, classes, "CZK");
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.baseEntryFee()).isNotNull();
            assertThat(result.baseEntryFee().amount()).isEqualByComparingTo(new BigDecimal("250"));
            assertThat(result.baseEntryFee().currency()).isEqualTo(Currency.getInstance("CZK"));
        }

        @Test
        @DisplayName("should ignore empty and unparseable fee values")
        void shouldIgnoreEmptyAndUnparseableFees() {
            int orisId = 3002;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Map<String, EventClass> classes = Map.of(
                    "M21", mockClassWithFee("M21", "300"),
                    "W21", mockClassWithFee("W21", ""),
                    "M35", mockClassWithFee("M35", "N/A")
            );
            EventDetails details = buildEventDetailsWithClassesAndCurrency(orisId, "Partial Fee Race", LocalDate.of(2026, 9, 1),
                    "Forest", org1, classes, "CZK");
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.baseEntryFee()).isNotNull();
            assertThat(result.baseEntryFee().amount()).isEqualByComparingTo(new BigDecimal("300"));
        }

        @Test
        @DisplayName("should return null baseEntryFee when all fees are empty or unparseable")
        void shouldReturnNullBaseEntryFeeWhenNoParseableFees() {
            int orisId = 3003;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Map<String, EventClass> classes = Map.of(
                    "M21", mockClassWithFee("M21", ""),
                    "W21", mockClassWithFee("W21", "free")
            );
            EventDetails details = buildEventDetailsWithClassesAndCurrency(orisId, "No Fee Race", LocalDate.of(2026, 9, 1),
                    "Forest", org1, classes, "CZK");
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.baseEntryFee()).isNull();
        }

        @Test
        @DisplayName("should return null baseEntryFee when classes are null")
        void shouldReturnNullBaseEntryFeeWhenClassesNull() {
            int orisId = 3004;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetails(orisId, "No Classes Race", LocalDate.of(2026, 9, 1), "Forest", org1, null);
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.baseEntryFee()).isNull();
        }

        @Test
        @DisplayName("should use CZK as default currency when currency is blank")
        void shouldUseCzkAsDefaultWhenCurrencyIsBlank() {
            int orisId = 3005;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Map<String, EventClass> classes = Map.of("M21", mockClassWithFee("M21", "150"));
            EventDetails details = buildEventDetailsWithClassesAndCurrency(orisId, "No Currency Race", LocalDate.of(2026, 9, 1),
                    "Forest", org1, classes, "");
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.baseEntryFee()).isNotNull();
            assertThat(result.baseEntryFee().currency()).isEqualTo(Currency.getInstance("CZK"));
        }

        @Test
        @DisplayName("should use CZK as default currency when currency is invalid")
        void shouldUseCzkAsDefaultWhenCurrencyIsInvalid() {
            int orisId = 3006;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            Map<String, EventClass> classes = Map.of("M21", mockClassWithFee("M21", "150"));
            EventDetails details = buildEventDetailsWithClassesAndCurrency(orisId, "Invalid Currency Race", LocalDate.of(2026, 9, 1),
                    "Forest", org1, classes, "INVALID");
            when(orisWebUrls.eventUrl(orisId)).thenReturn("https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

            OrisEventFields result = OrisEventDetailsMapper.map(details, orisId, orisWebUrls, null);

            assertThat(result.baseEntryFee()).isNotNull();
            assertThat(result.baseEntryFee().currency()).isEqualTo(Currency.getInstance("CZK"));
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
                                                        Map<String, EventClass> classes) {
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

    private EventClass mockClass(String orisClassId, String name) {
        EventClass cls = Mockito.mock(EventClass.class);
        Mockito.lenient().when(cls.id()).thenReturn(orisClassId);
        Mockito.when(cls.name()).thenReturn(name);
        return cls;
    }

    private EventClass mockClassWithFee(String name, String fee) {
        EventClass cls = Mockito.mock(EventClass.class);
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
                                                                   Map<String, EventClass> classes,
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
