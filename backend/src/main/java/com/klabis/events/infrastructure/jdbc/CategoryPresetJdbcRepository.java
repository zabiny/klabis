package com.klabis.events.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface CategoryPresetJdbcRepository extends CrudRepository<CategoryPresetMemento, UUID> {
}
