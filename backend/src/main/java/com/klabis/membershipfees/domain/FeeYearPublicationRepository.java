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
}
