package com.klabis.oris.eventsync;

import com.klabis.events.WebsiteUrl;
import com.klabis.events.application.OrisEventFields;
import com.klabis.events.domain.*;
import com.klabis.sync.infrastructure.SyncProjectionCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrisEventProjectionMapper")
class OrisEventProjectionMapperTest {

    @Test
    @DisplayName("maps an Event's ORIS-owned fields into the projection")
    void fromEvent_mapsOrisOwnedFields() {
        Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(1234)
                .name("Spring Sprint")
                .eventDate(LocalDate.of(2026, 5, 1))
                .location("Brno Park")
                .organizer("OOB")
                .websiteUrl(WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=1234"))
                .registrationDeadlines(RegistrationDeadlines.of(
                        LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15), null))
                .categories(List.of(com.klabis.events.EventCategory.createFromOris("101", "M21")))
                .ranking(EventRanking.of(1, "A", "Mistrovství republiky"))
                .baseEntryFee(Money.of(new BigDecimal("100.00"), Currency.getInstance("CZK")))
                .build());

        OrisEventProjection projection = OrisEventProjectionMapper.fromEvent(event);

        assertThat(projection.name()).isEqualTo("Spring Sprint");
        assertThat(projection.eventDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(projection.location()).isEqualTo("Brno Park");
        assertThat(projection.organizer()).isEqualTo("OOB");
        assertThat(projection.websiteUrl()).isEqualTo("https://oris.ceskyorientak.cz/Zavod?id=1234");
        assertThat(projection.registrationDeadline1()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(projection.registrationDeadline2()).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(projection.registrationDeadline3()).isNull();
        assertThat(projection.categories()).containsExactly(new OrisEventProjection.Category("101", "M21"));
        assertThat(projection.rankingLevelId()).isEqualTo(1);
        assertThat(projection.rankingShortName()).isEqualTo("A");
        assertThat(projection.rankingName()).isEqualTo("Mistrovství republiky");
        assertThat(projection.baseEntryFeeAmount()).isEqualByComparingTo("100.00");
        assertThat(projection.baseEntryFeeCurrency()).isEqualTo("CZK");
    }

    @Test
    @DisplayName("omits manually added categories, which are Klabis-owned")
    void fromEvent_omitsManuallyAddedCategories() {
        Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(1234)
                .name("Spring Sprint")
                .eventDate(LocalDate.of(2026, 5, 1))
                .location("Brno Park")
                .organizer("OOB")
                .websiteUrl(WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=1234"))
                .categories(List.of(
                        com.klabis.events.EventCategory.createFromOris("101", "M21"),
                        com.klabis.events.EventCategory.create("Custom category")))
                .build());

        OrisEventProjection projection = OrisEventProjectionMapper.fromEvent(event);

        assertThat(projection.categories()).containsExactly(new OrisEventProjection.Category("101", "M21"));
    }

    @Test
    @DisplayName("mapping the Event side and the ORIS side of equal data hashes equally")
    void fromEvent_andFromOrisFields_withEqualData_hashEqually() {
        Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(1234)
                .name("Spring Sprint")
                .eventDate(LocalDate.of(2026, 5, 1))
                .location("Brno Park")
                .organizer("OOB")
                .websiteUrl(WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=1234"))
                .registrationDeadlines(RegistrationDeadlines.of(
                        LocalDate.of(2026, 4, 1), null, null))
                .categories(List.of(com.klabis.events.EventCategory.createFromOris("101", "M21")))
                .ranking(EventRanking.of(1, "A", "Mistrovství republiky"))
                .baseEntryFee(Money.of(new BigDecimal("100"), Currency.getInstance("CZK")))
                .build());

        OrisEventFields orisFields = new OrisEventFields(
                "Spring Sprint",
                LocalDate.of(2026, 5, 1),
                "Brno Park",
                "OOB",
                WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=1234"),
                RegistrationDeadlines.of(LocalDate.of(2026, 4, 1), null, null),
                List.of(com.klabis.events.EventCategory.createFromOris("101", "M21")),
                EventRanking.of(1, "A", "Mistrovství republiky"),
                // Different scale from the Event side's Money("100") — must still hash equally.
                Money.of(new BigDecimal("100.00"), Currency.getInstance("CZK")),
                null
        );

        OrisEventProjection fromEventSide = OrisEventProjectionMapper.fromEvent(event);
        OrisEventProjection fromOrisSide = OrisEventFieldsToProjectionMapper.fromOrisFields(orisFields);

        assertThat(SyncProjectionCodec.hash(fromEventSide)).isEqualTo(SyncProjectionCodec.hash(fromOrisSide));
    }

    @Test
    @DisplayName("a changed field on either side hashes differently")
    void fromEvent_andFromOrisFields_withDifferentData_hashDifferently() {
        Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(1234)
                .name("Spring Sprint")
                .eventDate(LocalDate.of(2026, 5, 1))
                .location("Brno Park")
                .organizer("OOB")
                .websiteUrl(WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=1234"))
                .baseEntryFee(Money.of(new BigDecimal("100"), Currency.getInstance("CZK")))
                .build());

        OrisEventFields orisFields = new OrisEventFields(
                "Spring Sprint",
                LocalDate.of(2026, 5, 1),
                "Brno Park",
                "OOB",
                WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=1234"),
                RegistrationDeadlines.none(),
                List.of(),
                null,
                Money.of(new BigDecimal("150.00"), Currency.getInstance("CZK")),
                null
        );

        OrisEventProjection fromEventSide = OrisEventProjectionMapper.fromEvent(event);
        OrisEventProjection fromOrisSide = OrisEventFieldsToProjectionMapper.fromOrisFields(orisFields);

        assertThat(SyncProjectionCodec.hash(fromEventSide)).isNotEqualTo(SyncProjectionCodec.hash(fromOrisSide));
    }
}
