package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * Orchestrates the ORIS import and sync flows; the ORIS field primitives live on
 * {@link OrisEventFieldsGateway}.
 */
@PrimaryPort
public interface OrisEventImportPort {

    Event importEventFromOris(int orisId);

    void syncEventFromOris(EventId eventId);
}
