package com.klabis.events.domain;

import com.klabis.events.EventId;
import com.klabis.sync.domain.DependencyNotReadyException;

/**
 * An ORIS entry names a class the Klabis event does not have yet. Retryable: the event's own
 * synchronisation brings the category in, after which the entry can be taken over.
 */
public class OrisCategoryNotYetSynchronisedException extends DependencyNotReadyException {

    public OrisCategoryNotYetSynchronisedException(EventId eventId, String categoryOrisId) {
        super("Event " + eventId + " has no category with ORIS class id '" + categoryOrisId + "' yet");
    }
}
