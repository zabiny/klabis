package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FeeSelectionCampaignJdbcRepository extends CrudRepository<FeeSelectionCampaignMemento, UUID> {

    @Query("SELECT * FROM membershipfees.fee_selection_campaign WHERE publication_year = :year")
    Optional<FeeSelectionCampaignMemento> findByYear(@Param("year") int year);

    @Query("SELECT * FROM membershipfees.fee_selection_campaign WHERE voting_deadline < :today AND deadline_processed_at IS NULL")
    List<FeeSelectionCampaignMemento> findUnprocessedClosedPublications(@Param("today") LocalDate today);

    @Query("SELECT * FROM membershipfees.fee_selection_campaign WHERE voting_deadline >= :today LIMIT 1")
    Optional<FeeSelectionCampaignMemento> findActive(@Param("today") LocalDate today);
}
