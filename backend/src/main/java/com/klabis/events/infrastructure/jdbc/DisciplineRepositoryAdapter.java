package com.klabis.events.infrastructure.jdbc;

import com.klabis.events.DisciplineId;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;

import java.util.Optional;

@SecondaryAdapter
@org.jmolecules.ddd.annotation.Repository
class DisciplineRepositoryAdapter implements DisciplineRepository {

    private final DisciplineJdbcRepository jdbcRepository;

    DisciplineRepositoryAdapter(DisciplineJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public Discipline save(Discipline discipline) {
        return jdbcRepository.save(DisciplineMemento.from(discipline)).toDiscipline();
    }

    @Override
    public Optional<Discipline> findById(DisciplineId id) {
        return jdbcRepository.findById(id.value()).map(DisciplineMemento::toDiscipline);
    }
}
