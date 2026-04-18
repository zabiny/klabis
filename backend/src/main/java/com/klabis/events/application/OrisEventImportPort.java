package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface OrisEventImportPort {

    Event importEventFromOris(int orisId);

    void syncEventFromOris(EventId eventId);
}
