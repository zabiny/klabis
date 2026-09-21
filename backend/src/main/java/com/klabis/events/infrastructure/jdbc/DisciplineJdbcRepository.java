package com.klabis.events.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface DisciplineJdbcRepository extends CrudRepository<DisciplineMemento, UUID>, PagingAndSortingRepository<DisciplineMemento, UUID> {

    List<DisciplineMemento> findAllByOrderByNameAsc();

    // findAll(Pageable) is inherited from PagingAndSortingRepository
}
