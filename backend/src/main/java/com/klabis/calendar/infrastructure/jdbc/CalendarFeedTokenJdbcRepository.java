package com.klabis.calendar.infrastructure.jdbc;

import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@Repository
interface CalendarFeedTokenJdbcRepository extends CrudRepository<CalendarFeedTokenMemento, UUID> {

    List<CalendarFeedTokenMemento> findByTokenLookup(String tokenLookup);
}
