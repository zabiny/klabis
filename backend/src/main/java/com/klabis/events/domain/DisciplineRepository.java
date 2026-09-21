package com.klabis.events.domain;

import com.klabis.events.DisciplineId;

import java.util.Optional;

public interface DisciplineRepository {

    Discipline save(Discipline discipline);

    Optional<Discipline> findById(DisciplineId id);
}
