package com.klabis.events.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventId;

/**
 * {@code syncEventFromOris} was called on an event whose synchronisation record is
 * {@code CONFLICT} or {@code FAILED} — it needs a decision (resolve the conflict or
 * reset) via the synchronisation resource before an ordinary sync can run again
 * (design.md D18, "Existing operations, unchanged in shape").
 * <p>
 * Deliberately its own type rather than a direct throw of
 * {@code sync.application.SyncRecordNeedsResolutionException}: the {@code events}
 * module's REST layer should not need to know the sync module's internal exception
 * vocabulary, and {@code events} already owns {@code EventsExceptionHandler} for its
 * own 409 mappings.
 */
public class EventSyncNeedsResolutionException extends BusinessRuleViolationException {

    public EventSyncNeedsResolutionException(EventId eventId) {
        super(message("Event " + eventId));
    }

    /**
     * For {@code importEventFromOris} (task 9.4): {@code pullAndEnroll} refuses
     * before returning a {@code SyncRecord} when the existing pairing already awaits a
     * decision, so this path never learns the paired event's {@link EventId} — only
     * the ORIS id the caller supplied. The message still points at the synchronisation
     * resource concept; the caller is expected to find the specific event through
     * {@code GET /api/events?orisId=...} or the events list, same as any other
     * duplicate-import discovery.
     */
    public EventSyncNeedsResolutionException(int orisId) {
        super(message("ORIS event " + orisId));
    }

    private static String message(String subject) {
        return subject + " is not in sync and needs a decision (resolve the conflict or reset) via its synchronisation resource before it can be synchronised again";
    }
}
