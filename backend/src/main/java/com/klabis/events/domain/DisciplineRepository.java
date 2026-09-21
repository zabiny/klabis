package com.klabis.events.domain;

import com.klabis.events.DisciplineId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface DisciplineRepository {

    Discipline save(Discipline discipline);

    Optional<Discipline> findById(DisciplineId id);

    // Not archived-filtered yet: Discipline has no `archived` field until task 7.1/7.2 adds it.
    List<Discipline> findAllSorted();

    /**
     * Paginated listing backing the future {@code GET /api/disciplines} (design.md D10)
     * — a separate method from {@link #findAllSorted()}, which stays unpaged for the
     * {@code EventType} discipline picklist. Lists every discipline, active and
     * archived alike, in the order given by {@code pageable}'s sort.
     */
    Page<Discipline> findAll(Pageable pageable);
}
