package com.klabis.events.infrastructure.orissync;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.klabis.events.domain.OrisRegistrationId;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;

import java.time.Instant;

/**
 * The ORIS-owned fields of one participation (one member on one event), the same shape on both sides.
 * <p>
 * Presence is part of the projection: a side without a registration (or entry) is {@link #absent()}, so two
 * absent sides compare equal and a deletion on one side is an ordinary change of that side. Both sides are
 * normalised to the same shape before hashing, so a value Klabis cannot hold (SI card 0, an over-long note)
 * does not raise a difference on every pass.
 * <p>
 * {@code registeredAt} and {@code orisId} are carried for {@code createLocal} only and are
 * {@link JsonIgnore}d, so they never reach the hash or the stored snapshot.
 */
public record OrisEventRegistrationProjection(
        boolean present,
        String categoryOrisId,
        String siCardNumber,
        boolean wantsToRentSi,
        String orisNote,
        @JsonIgnore Instant registeredAt,
        @JsonIgnore OrisRegistrationId orisId
) implements SyncProjection {

    public static OrisEventRegistrationProjection absent() {
        return new OrisEventRegistrationProjection(false, null, null, false, null, null, null);
    }

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.EVENT_REGISTRATION;
    }
}
