package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * Orchestrates the ORIS import and sync flows. The ORIS field primitives the
 * synchronisation engine consumes live on {@link OrisEventFieldsGateway}, which —
 * unlike this port — does not depend on the sync engine, keeping the engine's
 * bean wiring acyclic.
 */
@PrimaryPort
public interface OrisEventImportPort {

    Event importEventFromOris(int orisId);

    void syncEventFromOris(EventId eventId);
}
