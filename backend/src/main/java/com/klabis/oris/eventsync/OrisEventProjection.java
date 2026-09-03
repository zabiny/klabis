package com.klabis.oris.eventsync;

import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The ORIS-owned event fields, in the shape shared by both the Klabis {@code Event}
 * side and the ORIS {@code EventDetails} side (design.md D3).
 * <p>
 * Klabis-owned fields — a category fee override, a manually added category, the event
 * type — are deliberately absent: they are invisible to synchronisation by
 * construction, mirroring today's field-ownership protection in
 * {@code Event.syncFromOris}.
 * <p>
 * Fields are plain JSON-friendly types rather than the domain value objects
 * ({@code RegistrationDeadlines}, {@code Money}, {@code EventRanking}) that carry them
 * on the {@code Event} aggregate: {@link SyncProjection} is a plain data carrier
 * serialised directly by {@code SyncProjectionCodec}, and value objects built on
 * {@code Optional} components serialise unpredictably without extra Jackson wiring the
 * codec deliberately does not carry.
 */
public record OrisEventProjection(
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        String websiteUrl,
        LocalDate registrationDeadline1,
        LocalDate registrationDeadline2,
        LocalDate registrationDeadline3,
        List<Category> categories,
        Integer rankingLevelId,
        String rankingShortName,
        String rankingName,
        BigDecimal baseEntryFeeAmount,
        String baseEntryFeeCurrency
) implements SyncProjection {

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.EVENT;
    }

    /**
     * A category as ORIS knows it: identified by its ORIS id, carrying only the
     * ORIS-owned name. A category's local id and fee override belong to Klabis and are
     * absent here — the same reason the projection as a whole omits Klabis-owned
     * fields.
     */
    public record Category(String orisId, String name) {
    }
}
