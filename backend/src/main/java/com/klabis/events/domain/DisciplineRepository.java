package com.klabis.events.domain;

import com.klabis.events.DisciplineId;

import java.util.List;
import java.util.Optional;

public interface DisciplineRepository {

    Discipline save(Discipline discipline);

    Optional<Discipline> findById(DisciplineId id);

    // Not archived-filtered yet: Discipline has no `archived` field until task 7.1/7.2 adds it.
    List<Discipline> findAllSorted();
}
