package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventClass;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Level;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventCategory;
import com.klabis.events.EventTypeId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventSyncDataConverter")
class EventSyncDataConverterTest {

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private OrisWebUrls orisWebUrls;

    private EventSyncDataConverter converter;

    @BeforeEach
    void setUp() {
        OrisEventMappingSupport support = new OrisEventMappingSupport(eventTypeRepository);
        OrisEventDetailsMapper mapper = Mappers.getMapper(OrisEventDetailsMapper.class);
        converter = new EventSyncDataConverter(mapper, support, orisWebUrls);
        Mockito.lenient().when(orisWebUrls.eventUrl(anyInt()))
                .thenAnswer(inv -> "https://oris.ceskyorientak.cz/Zavod?id=" + inv.getArgument(0));
    }

    private EventSyncData convert(int orisId, EventDetails details) {
        return converter.convert(new OrisEventSyncData(orisId, details));
    }

    @Test
    @DisplayName("maps trivial name / date / location fields")
    void mapsTrivialFields() {
        EventDetails details = details(123, "Spring Sprint");
        when(details.date()).thenReturn(LocalDate.of(2026, 8, 15));
        when(details.place()).thenReturn("Brno Park");

        EventSyncData result = convert(123, details);

        assertThat(result.eventId()).isNull();
        assertThat(result.orisId()).isEqualTo(123);
        assertThat(result.name()).isEqualTo("Spring Sprint");
        assertThat(result.eventDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(result.location()).isEqualTo("Brno Park");
    }

    @Test
    @DisplayName("websiteUrl is built from OrisWebUrls.eventUrl(orisId)")
    void websiteUrlFromOrisWebUrls() {
        EventSyncData result = convert(777, details(777, "Race"));

        assertThat(result.websiteUrl())
                .isEqualTo(WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=777"));
    }

    @Nested
    @DisplayName("organizer")
    class OrganizerResolution {

        @Test
        @DisplayName("uses org1 abbreviation when present")
        void usesOrg1() {
            EventDetails details = details(1, "Race");
            when(details.org1()).thenReturn(new Organizer(205, "OOB", "Orel Brno"));

            assertThat(convert(1, details).organizer()).isEqualTo("OOB");
        }

        @Test
        @DisplayName("falls back to org2 abbreviation when org1 is blank")
        void fallsBackToOrg2() {
            EventDetails details = details(1, "Race");
            when(details.org1()).thenReturn(new Organizer(1, "", "Empty"));
            when(details.org2()).thenReturn(new Organizer(205, "PRG", "Prague OB"));

            assertThat(convert(1, details).organizer()).isEqualTo("PRG");
        }

        @Test
        @DisplayName("falls back to '---' when both organizers are blank")
        void unknownOrganizer() {
            EventDetails details = details(1, "Race");
            when(details.org1()).thenReturn(new Organizer(1, null, "Unnamed"));
            when(details.org2()).thenReturn(null);

            assertThat(convert(1, details).organizer()).isEqualTo("---");
        }
    }

    @Nested
    @DisplayName("registration deadlines")
    class Deadlines {

        @Test
        @DisplayName("maps a single EntryDate1")
        void singleDeadline() {
            LocalDate d1 = LocalDate.of(2026, 5, 15);
            EventDetails details = details(1001, "Race");
            when(details.entryDate1()).thenReturn(d1.atStartOfDay(ZoneId.of("Europe/Prague")));

            var deadlines = convert(1001, details).registrationDeadlines();

            assertThat(deadlines.deadline1()).contains(d1);
            assertThat(deadlines.deadline2()).isEmpty();
            assertThat(deadlines.deadline3()).isEmpty();
        }

        @Test
        @DisplayName("out-of-order deadlines raise BusinessRuleViolationException")
        void outOfOrderThrows() {
            LocalDate d1 = LocalDate.of(2026, 4, 1);
            LocalDate d3 = LocalDate.of(2026, 6, 1);
            EventDetails details = details(1003, "Race");
            when(details.entryDate1()).thenReturn(d1.atStartOfDay(ZoneId.of("Europe/Prague")));
            when(details.entryDate3()).thenReturn(d3.atStartOfDay(ZoneId.of("Europe/Prague")));

            assertThatThrownBy(() -> convert(1003, details))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("invalid registration deadlines");
        }
    }

    @Nested
    @DisplayName("categories")
    class Categories {

        @Test
        @DisplayName("maps ORIS classes and skips blank names")
        void mapsAndSkipsBlank() {
            EventClass m21 = mockClass("100", "M21");
            EventClass blank = mockClass("101", "  ");
            EventDetails details = details(1, "Race");
            when(details.classes()).thenReturn(Map.of("100", m21, "101", blank));

            var categories = convert(1, details).categories();

            assertThat(categories).extracting(EventCategory::name).containsExactly("M21");
            assertThat(categories.get(0).orisId()).isEqualTo("100");
        }
    }

    @Nested
    @DisplayName("ranking")
    class Ranking {

        @Test
        @DisplayName("maps ORIS Level to EventRanking")
        void mapsLevel() {
            EventDetails details = details(1, "Race");
            when(details.level()).thenReturn(new Level(3, "MČR", "Mistrovství ČR", "Czech Championships"));

            var ranking = convert(1, details).ranking();

            assertThat(ranking).isNotNull();
            assertThat(ranking.levelId()).isEqualTo(3);
            assertThat(ranking.shortName()).isEqualTo("MČR");
        }

        @Test
        @DisplayName("null when level is missing")
        void nullLevel() {
            assertThat(convert(1, details(1, "Race")).ranking()).isNull();
        }
    }

    @Nested
    @DisplayName("base entry fee")
    class BaseEntryFee {

        @Test
        @DisplayName("derives MAX fee across classes")
        void maxFee() {
            EventClass m21 = mockClassWithFee("M21", "250");
            EventClass w21 = mockClassWithFee("W21", "200");
            EventDetails details = details(1, "Race");
            when(details.classes()).thenReturn(Map.of("M21", m21, "W21", w21));
            when(details.currency()).thenReturn("CZK");

            var fee = convert(1, details).baseEntryFee();

            assertThat(fee).isNotNull();
            assertThat(fee.amount()).isEqualByComparingTo(new BigDecimal("250"));
            assertThat(fee.currency()).isEqualTo(Currency.getInstance("CZK"));
        }
    }

    @Nested
    @DisplayName("auto-mapped event type")
    class AutoMappedEventType {

        @Test
        @DisplayName("null for discipline id <= 0 (ORIS sentinel)")
        void sentinelDiscipline() {
            EventDetails details = details(1, "Race");
            when(details.discipline()).thenReturn(new Discipline(0, "x", "x", "x"));

            assertThat(convert(1, details).autoMappedEventType()).isNull();
        }

        @Test
        @DisplayName("looks up event type by ORIS discipline id")
        void looksUpByDisciplineId() {
            EventTypeId typeId = EventTypeId.of(UUID.randomUUID());
            EventType eventType = Mockito.mock(EventType.class);
            when(eventType.getId()).thenReturn(typeId);
            when(eventTypeRepository.findByOrisDisciplineId(5)).thenReturn(Optional.of(eventType));

            EventDetails details = details(1, "Race");
            when(details.discipline()).thenReturn(new Discipline(5, "OB", "OB", "OB"));

            assertThat(convert(1, details).autoMappedEventType()).isEqualTo(typeId);
        }

        @Test
        @DisplayName("null when no event type is mapped to the discipline id")
        void noMapping() {
            when(eventTypeRepository.findByOrisDisciplineId(7)).thenReturn(Optional.empty());

            EventDetails details = details(1, "Race");
            when(details.discipline()).thenReturn(new Discipline(7, "OB", "OB", "OB"));

            assertThat(convert(1, details).autoMappedEventType()).isNull();
        }
    }

    private EventDetails details(int orisId, String name) {
        EventDetails details = Mockito.mock(EventDetails.class);
        Mockito.lenient().when(details.name()).thenReturn(name);
        Mockito.lenient().when(details.date()).thenReturn(LocalDate.of(2026, 8, 15));
        Mockito.lenient().when(details.place()).thenReturn("Somewhere");
        Mockito.lenient().when(details.currency()).thenReturn(null);
        Mockito.lenient().when(details.org1()).thenReturn(new Organizer(205, "OOB", "Orel Brno"));
        Mockito.lenient().when(details.org2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate1()).thenReturn(null);
        Mockito.lenient().when(details.entryDate2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate3()).thenReturn(null);
        Mockito.lenient().when(details.classes()).thenReturn(null);
        Mockito.lenient().when(details.level()).thenReturn(null);
        Mockito.lenient().when(details.discipline()).thenReturn(null);
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
        Mockito.lenient().when(cls.name()).thenReturn(name);
        Mockito.when(cls.fee()).thenReturn(fee);
        return cls;
    }
}
