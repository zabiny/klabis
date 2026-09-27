package com.klabis.events.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;

/**
 * {@code importEventFromOris} was called for an ORIS event whose synchronisation record is
 * {@code CONFLICT} or {@code FAILED} and needs a decision via its synchronisation resource
 * before an ordinary sync can run again (design.md D18).
 */
public class EventSyncNeedsResolutionException extends BusinessRuleViolationException {

    public EventSyncNeedsResolutionException(int orisId) {
        super(message("ORIS event " + orisId));
    }

    private static String message(String subject) {
        return subject + " is not in sync and needs a decision (resolve the conflict or reset) via its synchronisation resource before it can be synchronised again";
    }
}
