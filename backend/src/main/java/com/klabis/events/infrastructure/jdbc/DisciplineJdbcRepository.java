package com.klabis.events.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface DisciplineJdbcRepository extends CrudRepository<DisciplineMemento, UUID> {

    List<DisciplineMemento> findAllByOrderByNameAsc();
}
