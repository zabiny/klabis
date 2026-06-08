package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface FeeYearPublicationJdbcRepository extends CrudRepository<FeeYearPublicationMemento, UUID> {

    @Query("SELECT * FROM fee_year_publication WHERE publication_year = :year")
    Optional<FeeYearPublicationMemento> findByYear(@Param("year") int year);
}
