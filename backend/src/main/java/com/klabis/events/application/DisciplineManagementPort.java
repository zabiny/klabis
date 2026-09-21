package com.klabis.events.application;

import com.klabis.events.DisciplineId;
import com.klabis.events.domain.Discipline;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@PrimaryPort
public interface DisciplineManagementPort {

    Discipline create(Discipline.CreateDiscipline command);

    Discipline get(DisciplineId id);

    Discipline update(DisciplineId id, String code, String name);

    void archive(DisciplineId id);

    /**
     * Restores an archived discipline (design.md D8) and, if it was ORIS-paired,
     * reactivates the sync pairing.
     *
     * @param actingUser opaque identifier of the restoring user (mirrors {@code
     *                    SynchronizationPort}'s own {@code actingUser} convention),
     *                    carried through to {@code pullAndEnroll} if reactivation
     *                    is needed
     */
    void restore(DisciplineId id, String actingUser);

    Page<Discipline> list(Pageable pageable);
}
