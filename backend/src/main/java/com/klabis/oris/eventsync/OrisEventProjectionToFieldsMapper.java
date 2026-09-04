package com.klabis.oris.eventsync;

import com.klabis.events.EventCategory;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.application.OrisEventFields;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;

import java.util.Currency;

/**
 * Maps a synchronised {@link OrisEventProjection} back into {@link OrisEventFields},
 * the shape {@link com.klabis.events.application.OrisEventImportPort#applyOrisSync}
 * writes via {@code Event.syncFromOris} (design.md D2).
 * <p>
 * The projection is what the engine decided to write inward — it may be the freshly
 * read external side, or (after a resolved conflict) a snapshot the engine already
 * holds — so this mapper never talks to ORIS itself; it only reshapes data the engine
 * already has, including {@link OrisEventProjection#resolvedEventTypeId()}, which
 * travels on the projection itself rather than through separate thread-local state
 * (see the field's javadoc on {@link OrisEventProjection}).
 */
final class OrisEventProjectionToFieldsMapper {

    private OrisEventProjectionToFieldsMapper() {
    }

    static OrisEventFields toOrisEventFields(OrisEventProjection projection) {
        RegistrationDeadlines deadlines = RegistrationDeadlines.of(
                projection.registrationDeadline1(),
                projection.registrationDeadline2(),
                projection.registrationDeadline3());

        EventRanking ranking = projection.rankingLevelId() != null
                ? EventRanking.of(projection.rankingLevelId(), projection.rankingShortName(), projection.rankingName())
                : null;

        Money baseEntryFee = projection.baseEntryFeeAmount() != null
                ? Money.of(projection.baseEntryFeeAmount(), Currency.getInstance(projection.baseEntryFeeCurrency()))
                : null;

        return new OrisEventFields(
                projection.name(),
                projection.eventDate(),
                projection.location(),
                projection.organizer(),
                projection.websiteUrl() != null ? WebsiteUrl.of(projection.websiteUrl()) : null,
                deadlines,
                projection.categories().stream()
                        .map(category -> EventCategory.createFromOris(category.orisId(), category.name()))
                        .toList(),
                ranking,
                baseEntryFee,
                projection.resolvedEventTypeId()
        );
    }
}
