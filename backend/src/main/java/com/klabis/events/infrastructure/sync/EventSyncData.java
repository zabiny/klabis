package com.klabis.events.infrastructure.sync;

import com.klabis.common.sync.SyncData;
import com.klabis.events.EventCategory;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;

import java.time.LocalDate;
import java.util.List;

/**
 * Local-side payload of the ORIS event {@code SyncLine}: a flat, already-translated view of an event
 * ready to be turned into (or merged onto) a Klabis {@link com.klabis.events.domain.Event}. All ORIS
 * DTO translation has happened before this record is built (see {@link EventSyncDataConverter}), so
 * {@link LocalEventSyncSource} only does create-vs-update resolution and command assembly.
 * <p>
 * {@code eventId} is {@code null} on the PULL (import/resync) path — the local event is resolved by
 * {@code orisId} — and populated on the (dead) PUSH path where the local source produces it.
 * {@code autoMappedEventType} is {@code null} on the PUSH path.
 */
public record EventSyncData(
        EventId eventId,
        int orisId,
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        WebsiteUrl websiteUrl,
        RegistrationDeadlines registrationDeadlines,
        List<EventCategory> categories,
        EventRanking ranking,
        Money baseEntryFee,
        EventTypeId autoMappedEventType
) implements SyncData {
}
