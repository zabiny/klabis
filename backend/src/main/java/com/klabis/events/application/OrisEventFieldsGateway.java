package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * The ORIS field primitives of the events module's ORIS integration, kept apart from
 * the flow orchestration on {@link OrisEventImportPort} so the synchronisation engine
 * can depend on them without depending on the engine itself: this gateway never
 * reaches {@code com.klabis.sync}, so wiring it into the engine's adapters is eager
 * and acyclic.
 */
@PrimaryPort
public interface OrisEventFieldsGateway {

    /**
     * Reads and maps an ORIS event's ORIS-owned fields, without writing anything —
     * the synchronisation engine's external-side read (design.md D2, D3). Resolves
     * the same way {@link OrisEventImportPort#importEventFromOris} does, including
     * event-type auto-mapping from the ORIS discipline.
     */
    OrisEventFields readOrisFields(int orisId);

    /**
     * Writes an already-mapped {@link OrisEventFields} bundle onto the given event via
     * {@code Event.syncFromOris}, preserving its category merge and event-type
     * behaviour — the synchronisation engine's local-side write (design.md D2). Unlike
     * {@link OrisEventImportPort#syncEventFromOris}, this does not itself call ORIS: the
     * caller supplies the fields, so the same value obtained from {@link #readOrisFields}
     * elsewhere in a synchronisation pass is written verbatim rather than re-fetched.
     */
    Event applyOrisSync(EventId eventId, OrisEventFields fields);
}
