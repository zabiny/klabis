package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventClass;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Level;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventCategory;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeRepository;
import com.klabis.events.domain.Money;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventMappingSupport")
class OrisEventMappingSupportTest {

    @Mock
    private EventTypeRepository eventTypeRepository;

    private OrisEventMappingSupport support;

    @BeforeEach
    void setUp() {
        support = new OrisEventMappingSupport(eventTypeRepository);
    }

    @Nested
    @DisplayName("resolveOrganizer")
    class ResolveOrganizer {

        @Test
        @DisplayName("uses org1 abbreviation when present")
        void usesOrg1() {
            EventDetails details = Mockito.mock(EventDetails.class);
            when(details.org1()).thenReturn(new Organizer(205, "OOB", "Orel Brno"));

            assertThat(support.resolveOrganizer(details)).isEqualTo("OOB");
        }

        @Test
        @DisplayName("falls back to org2 abbreviation when org1 abbreviation is blank")
        void fallsBackToOrg2() {
            EventDetails details = Mockito.mock(EventDetails.class);
            when(details.org1()).thenReturn(new Organizer(1, "", "Empty Org"));
            when(details.org2()).thenReturn(new Organizer(205, "PRG", "Prague OB"));

            assertThat(support.resolveOrganizer(details)).isEqualTo("PRG");
        }

        @Test
        @DisplayName("returns '---' when both org abbreviations are blank or null")
        void unknownOrganizer() {
            EventDetails details = Mockito.mock(EventDetails.class);
            when(details.org1()).thenReturn(new Organizer(1, null, "Unnamed Org"));
            when(details.org2()).thenReturn(null);

            assertThat(support.resolveOrganizer(details)).isEqualTo("---");
        }
    }

    @Nested
    @DisplayName("buildRegistrationDeadlines")
    class BuildRegistrationDeadlines {

        @Test
        @DisplayName("maps a single EntryDate1 to one deadline")
        void singleDeadline() {
            LocalDate d1 = LocalDate.of(2026, 5, 15);
            EventDetails details = Mockito.mock(EventDetails.class);
            when(details.entryDate1()).thenReturn(d1.atStartOfDay(ZoneId.of("Europe/Prague")));
            when(details.entryDate2()).thenReturn(null);
            when(details.entryDate3()).thenReturn(null);

            var deadlines = support.buildRegistrationDeadlines(details, 1001);

            assertThat(deadlines.deadline1()).contains(d1);
            assertThat(deadlines.deadline2()).isEmpty();
            assertThat(deadlines.deadline3()).isEmpty();
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when EntryDate1 and EntryDate3 present but not EntryDate2")
        void outOfOrderThrows() {
            LocalDate d1 = LocalDate.of(2026, 4, 1);
            LocalDate d3 = LocalDate.of(2026, 6, 1);
            EventDetails details = Mockito.mock(EventDetails.class);
            when(details.entryDate1()).thenReturn(d1.atStartOfDay(ZoneId.of("Europe/Prague")));
            when(details.entryDate2()).thenReturn(null);
            when(details.entryDate3()).thenReturn(d3.atStartOfDay(ZoneId.of("Europe/Prague")));

            assertThatThrownBy(() -> support.buildRegistrationDeadlines(details, 1003))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("invalid registration deadlines");
        }
    }

    @Nested
    @DisplayName("extractCategories")
    class ExtractCategories {

        @Test
        @DisplayName("maps ORIS EventClass id and name to EventCategory")
        void mapsClasses() {
            EventClass m21 = mockClass("100", "M21");
            EventDetails details = Mockito.mock(EventDetails.class);
            when(details.classes()).thenReturn(Map.of("100", m21));

            var categories = support.extractCategories(details);

            assertThat(categories).hasSize(1);
            assertThat(categories.get(0).orisId()).isEqualTo("100");
            assertThat(categories.get(0).name()).isEqualTo("M21");
        }

        @Test
        @DisplayName("skips classes with a blank name")
        void skipsBlankNames() {
            EventClass m21 = mockClass("100", "M21");
            EventClass blank = mockClass("101", "  ");
            EventDetails details = Mockito.mock(EventDetails.class);
            when(details.classes()).thenReturn(Map.of("100", m21, "101", blank));

            var categories = support.extractCategories(details);

            assertThat(categories).extracting(EventCategory::name).containsExactly("M21");
        }

        @Test
        @DisplayName("returns empty list when classes are null")
        void nullClasses() {
            EventDetails details = Mockito.mock(EventDetails.class);
            when(details.classes()).thenReturn(null);

            assertThat(support.extractCategories(details)).isEmpty();
        }
    }

    @Nested
    @DisplayName("resolveRanking")
    class ResolveRanking {

        @Test
        @DisplayName("maps ORIS Level to EventRanking")
        void mapsLevel() {
            EventRanking ranking = support.resolveRanking(new Level(3, "MČR", "Mistrovství ČR", "Czech Championships"));

            assertThat(ranking).isNotNull();
            assertThat(ranking.levelId()).isEqualTo(3);
            assertThat(ranking.shortName()).isEqualTo("MČR");
            assertThat(ranking.name()).isEqualTo("Mistrovství ČR");
        }

        @Test
        @DisplayName("returns null when level is null")
        void nullLevel() {
            assertThat(support.resolveRanking(null)).isNull();
        }
    }

    @Nested
    @DisplayName("deriveBaseEntryFee")
    class DeriveBaseEntryFee {

        @Test
        @DisplayName("derives MAX fee across classes")
        void maxFee() {
            EventClass m21 = mockClassWithFee("M21", "250");
            EventClass w21 = mockClassWithFee("W21", "200");
            EventClass m35 = mockClassWithFee("M35", "180");
            EventDetails details = Mockito.mock(EventDetails.class);
            when(details.classes()).thenReturn(Map.of("M21", m21, "W21", w21, "M35", m35));
            when(details.currency()).thenReturn("CZK");

            Money fee = support.deriveBaseEntryFee(details);

            assertThat(fee).isNotNull();
            assertThat(fee.amount()).isEqualByComparingTo(new BigDecimal("250"));
            assertThat(fee.currency()).isEqualTo(Currency.getInstance("CZK"));
        }

        @Test
        @DisplayName("ignores empty and unparseable fee values")
        void ignoresUnparseable() {
            EventClass m21 = mockClassWithFee("M21", "300");
            EventClass w21 = mockClassWithFee("W21", "");
            EventClass m35 = mockClassWithFee("M35", "N/A");
            EventDetails details = Mockito.mock(EventDetails.class);
            when(details.classes()).thenReturn(Map.of("M21", m21, "W21", w21, "M35", m35));
            when(details.currency()).thenReturn("CZK");

            Money fee = support.deriveBaseEntryFee(details);

            assertThat(fee).isNotNull();
            assertThat(fee.amount()).isEqualByComparingTo(new BigDecimal("300"));
        }

        @Test
        @DisplayName("returns null when classes are null")
        void nullClasses() {
            EventDetails details = Mockito.mock(EventDetails.class);
            when(details.classes()).thenReturn(null);

            assertThat(support.deriveBaseEntryFee(details)).isNull();
        }
    }

    @Nested
    @DisplayName("resolveEventType")
    class ResolveEventType {

        @Test
        @DisplayName("returns null for discipline id <= 0 (ORIS sentinel)")
        void sentinelDiscipline() {
            assertThat(support.resolveEventType(new Discipline(0, "x", "x", "x"))).isNull();
        }

        @Test
        @DisplayName("returns null when discipline is null")
        void nullDiscipline() {
            assertThat(support.resolveEventType(null)).isNull();
        }

        @Test
        @DisplayName("looks up the event type by ORIS discipline id when id is positive")
        void looksUpByDisciplineId() {
            EventType eventType = Mockito.mock(EventType.class);
            EventTypeId typeId = EventTypeId.of(java.util.UUID.randomUUID());
            when(eventType.getId()).thenReturn(typeId);
            when(eventTypeRepository.findByOrisDisciplineId(5)).thenReturn(Optional.of(eventType));

            assertThat(support.resolveEventType(new Discipline(5, "OB", "OB", "OB"))).isEqualTo(typeId);
        }

        @Test
        @DisplayName("returns null when no event type is mapped to the discipline id")
        void noMapping() {
            when(eventTypeRepository.findByOrisDisciplineId(7)).thenReturn(Optional.empty());

            assertThat(support.resolveEventType(new Discipline(7, "OB", "OB", "OB"))).isNull();
        }
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
        Mockito.lenient().when(cls.name()).thenReturn(name);
        Mockito.when(cls.fee()).thenReturn(fee);
        return cls;
    }
}
