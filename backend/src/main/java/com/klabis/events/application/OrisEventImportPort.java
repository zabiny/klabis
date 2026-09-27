package com.klabis.events.application;

import com.klabis.events.domain.Event;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * Orchestrates the ORIS import flow; the ORIS field primitives live on
 * {@link OrisEventFieldsReader}.
 */
@PrimaryPort
public interface OrisEventImportPort {

    Event importEventFromOris(int orisId);
}
