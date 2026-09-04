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
        super("Event " + eventId + " is not in sync and needs a decision (resolve the conflict or reset) via its synchronisation resource before it can be synchronised again");
    }
}
