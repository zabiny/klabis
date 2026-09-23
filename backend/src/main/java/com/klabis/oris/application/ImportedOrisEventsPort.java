package com.klabis.oris.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Collection;
import java.util.Set;

/**
 * Lets the {@code oris} module filter its external event listing down to events the club has not
 * imported yet, without reaching into {@code events} itself.
 * <p>
 * The port is declared and owned by {@code oris} — the module that needs the answer — and
 * implemented by {@code events} ({@code ImportedOrisEventsService}), which owns the data. This
 * inverts the earlier direction where {@code oris} imported {@code events.application}'s port;
 * with the port here, {@code oris} has no outgoing edge into {@code events}, and the allowed
 * {@code events → oris} direction carries the implementation instead
 * (openspec {@code relocate-oris-event-sync-adapter}).
 */
@PrimaryPort
public interface ImportedOrisEventsPort {

    /**
     * Returns the subset of {@code candidateOrisIds} that are already imported in the events module.
     *
     * @param candidateOrisIds ORIS IDs to check; may be empty
     * @return imported IDs from the candidate set
     */
    Set<Integer> findImportedOrisIds(Collection<Integer> candidateOrisIds);
}
