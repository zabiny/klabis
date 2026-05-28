package com.klabis.events.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Collection;
import java.util.Set;

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
