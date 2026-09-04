package com.klabis.oris.eventsync;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.klabis.events.EventTypeId;
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
 * type — are deliberately absent from comparison: they are invisible to
 * synchronisation by construction, mirroring today's field-ownership protection in
 * {@code Event.syncFromOris}.
 * <p>
 * Fields are plain JSON-friendly types rather than the domain value objects
 * ({@code RegistrationDeadlines}, {@code Money}, {@code EventRanking}) that carry them
 * on the {@code Event} aggregate: {@link SyncProjection} is a plain data carrier
 * serialised directly by {@code SyncProjectionCodec}, and value objects built on
 * {@code Optional} components serialise unpredictably without extra Jackson wiring the
 * codec deliberately does not carry.
 * <p>
 * {@code resolvedEventTypeId} is the one exception to "plain JSON-friendly types":
 * it is {@link JsonIgnore}d so {@link com.klabis.sync.infrastructure.SyncProjectionCodec}
 * never serialises it — the event type is Klabis-owned (design.md D3) and must not
 * participate in hashing, persistence or field-level divergence, exactly like every
 * other Klabis-owned field. It carries the ORIS discipline resolution
 * ({@code events.application.OrisEventFields#resolvedEventTypeId}) from
 * {@link OrisEventSyncAdapter#readExternal} to {@link OrisEventSyncAdapter#applyToLocal}
 * on the external projection itself, so the value travels with the exact projection it
 * was resolved from instead of through thread-local state keyed by call order (task
 * 8.10's follow-up fix). Always {@code null} on the local-side projection
 * ({@link OrisEventProjectionMapper#fromEvent}): the event type is never read back out
 * of a local {@code Event} for synchronisation purposes.
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
        String baseEntryFeeCurrency,
        @JsonIgnore EventTypeId resolvedEventTypeId
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
