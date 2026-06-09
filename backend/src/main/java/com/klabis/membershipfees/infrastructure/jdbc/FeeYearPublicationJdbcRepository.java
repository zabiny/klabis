package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FeeYearPublicationJdbcRepository extends CrudRepository<FeeYearPublicationMemento, UUID> {

    @Query("SELECT * FROM membershipfees.fee_year_publication WHERE publication_year = :year")
    Optional<FeeYearPublicationMemento> findByYear(@Param("year") int year);

    @Query("SELECT * FROM membershipfees.fee_year_publication WHERE voting_deadline < :today AND deadline_processed_at IS NULL")
    List<FeeYearPublicationMemento> findUnprocessedClosedPublications(@Param("today") LocalDate today);
}
