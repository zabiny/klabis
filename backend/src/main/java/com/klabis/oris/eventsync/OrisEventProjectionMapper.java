package com.klabis.oris.eventsync;

import com.klabis.events.EventCategory;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;

import java.util.List;

/**
 * Maps the ORIS-owned fields of a Klabis {@link Event} into the canonical
 * {@link OrisEventProjection} shape (design.md D3).
 * <p>
 * The external ({@code EventDetails}) side is mapped by
 * {@code OrisEventFields}/{@code OrisEventDetailsMapper} in {@code events.application}
 * — the same mapping {@code OrisEventImportService} already uses — and then adapted
 * into this same projection shape by {@link OrisEventSyncAdapter}, so both sides of a
 * comparison go through one field-ownership rule.
 */
final class OrisEventProjectionMapper {

    private OrisEventProjectionMapper() {
    }

    static OrisEventProjection fromEvent(Event event) {
        RegistrationDeadlines deadlines = event.getRegistrationDeadlines();
        EventRanking ranking = event.getRanking();
        Money baseEntryFee = event.getBaseEntryFee();

        return new OrisEventProjection(
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null,
                deadlines != null ? deadlines.deadline1().orElse(null) : null,
                deadlines != null ? deadlines.deadline2().orElse(null) : null,
                deadlines != null ? deadlines.deadline3().orElse(null) : null,
                orisOriginCategories(event.getCategories()),
                ranking != null ? ranking.levelId() : null,
                ranking != null ? ranking.shortName() : null,
                ranking != null ? ranking.name() : null,
                baseEntryFee != null ? baseEntryFee.amount() : null,
                baseEntryFee != null ? baseEntryFee.currency().getCurrencyCode() : null,
                null
        );
    }

    private static List<OrisEventProjection.Category> orisOriginCategories(List<EventCategory> categories) {
        return categories.stream()
                .filter(category -> category.orisId() != null)
                .map(category -> new OrisEventProjection.Category(category.orisId(), category.name()))
                .toList();
    }
}
