package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface OrisEventImportPort {

    Event importEventFromOris(int orisId);

    void syncEventFromOris(EventId eventId);

    /**
     * Reads and maps an ORIS event's ORIS-owned fields, without writing anything —
     * the synchronisation engine's external-side read (design.md D2, D3). Resolves
     * the same way {@link #syncEventFromOris} does, including event-type auto-mapping
     * from the ORIS discipline.
     */
    OrisEventFields readOrisFields(int orisId);

    /**
     * Writes an already-mapped {@link OrisEventFields} bundle onto the given event via
     * {@code Event.syncFromOris}, preserving its category merge and event-type
     * behaviour — the synchronisation engine's local-side write (design.md D2). Unlike
     * {@link #syncEventFromOris}, this does not itself call ORIS: the caller supplies
     * the fields, so the same value obtained from {@link #readOrisFields} elsewhere in
     * a synchronisation pass is written verbatim rather than re-fetched.
     */
    Event applyOrisSync(EventId eventId, OrisEventFields fields);
}
