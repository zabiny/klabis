package com.klabis.events.application;

import com.klabis.events.EventCategory;
import com.klabis.events.EventTypeId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;

import java.time.LocalDate;
import java.util.List;

/**
 * The ORIS-owned event fields mapped out of an ORIS {@code EventDetails} response,
 * independent of any particular Klabis {@code Event} instance.
 * <p>
 * Shared by {@link OrisEventImportService} (import and manual sync) and by
 * {@code com.klabis.oris.sync.OrisEventSyncAdapter} — the synchronisation engine's
 * external-side read — so the ORIS field mapping exists exactly once (design.md D2,
 * D3).
 */
public record OrisEventFields(
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        WebsiteUrl websiteUrl,
        RegistrationDeadlines registrationDeadlines,
        List<EventCategory> categories,
        EventRanking ranking,
        Money baseEntryFee,
        EventTypeId resolvedEventTypeId
) {
}
