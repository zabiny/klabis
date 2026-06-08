package com.klabis.membershipfees.domain;

import com.klabis.membershipfees.FeeYearPublicationId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeYearPublicationRepository {

    FeeYearPublication save(FeeYearPublication publication);

    Optional<FeeYearPublication> findById(FeeYearPublicationId id);

    Optional<FeeYearPublication> findByYear(int year);

    List<FeeYearPublication> findAll();

    /**
     * Returns all publications whose voting deadline has passed (deadline < today)
     * and that have not yet been processed by the scheduler (deadlineProcessedAt is null).
     */
    List<FeeYearPublication> findUnprocessedClosedPublications(java.time.LocalDate today);
}
