package com.klabis.calendar.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface CalendarJdbcRepository extends CrudRepository<CalendarMemento, UUID> {
}
