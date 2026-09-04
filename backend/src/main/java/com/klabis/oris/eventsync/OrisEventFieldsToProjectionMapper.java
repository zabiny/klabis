package com.klabis.oris.eventsync;

import com.klabis.events.WebsiteUrl;
import com.klabis.events.application.OrisEventFields;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;

/**
 * Maps {@link OrisEventFields} — the ORIS-owned fields already mapped out of an ORIS
 * {@code EventDetails} response by {@code events.application} — into the canonical
 * {@link OrisEventProjection} shape used for the external side of a synchronisation
 * comparison (design.md D3).
 * <p>
 * Together with {@link OrisEventProjectionMapper#fromEvent}, this is what lets the
 * engine compare the Klabis side and the ORIS side as the same shape.
 */
final class OrisEventFieldsToProjectionMapper {

    private OrisEventFieldsToProjectionMapper() {
    }

    static OrisEventProjection fromOrisFields(OrisEventFields fields) {
        RegistrationDeadlines deadlines = fields.registrationDeadlines();
        EventRanking ranking = fields.ranking();
        Money baseEntryFee = fields.baseEntryFee();
        WebsiteUrl websiteUrl = fields.websiteUrl();

        return new OrisEventProjection(
                fields.name(),
                fields.eventDate(),
                fields.location(),
                fields.organizer(),
                websiteUrl != null ? websiteUrl.value() : null,
                deadlines != null ? deadlines.deadline1().orElse(null) : null,
                deadlines != null ? deadlines.deadline2().orElse(null) : null,
                deadlines != null ? deadlines.deadline3().orElse(null) : null,
                fields.categories().stream()
                        .map(category -> new OrisEventProjection.Category(category.orisId(), category.name()))
                        .toList(),
                ranking != null ? ranking.levelId() : null,
                ranking != null ? ranking.shortName() : null,
                ranking != null ? ranking.name() : null,
                baseEntryFee != null ? baseEntryFee.amount() : null,
                baseEntryFee != null ? baseEntryFee.currency().getCurrencyCode() : null,
                fields.resolvedEventTypeId()
        );
    }
}
