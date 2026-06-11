package com.klabis.membershipfees.domain;

import com.klabis.membershipfees.FeeSelectionCampaignId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeSelectionCampaignRepository {

    FeeSelectionCampaign save(FeeSelectionCampaign publication);

    Optional<FeeSelectionCampaign> findById(FeeSelectionCampaignId id);

    Optional<FeeSelectionCampaign> findByYear(int year);

    List<FeeSelectionCampaign> findAll();

    /**
     * Returns all publications whose voting deadline has passed (deadline < today)
     * and that have not yet been processed by the scheduler (deadlineProcessedAt is null).
     */
    List<FeeSelectionCampaign> findUnprocessedClosedPublications(java.time.LocalDate today);

    /**
     * Returns the active campaign (one whose voting deadline has not passed yet),
     * i.e. votingDeadline >= today.
     */
    java.util.Optional<FeeSelectionCampaign> findActive(java.time.LocalDate today);
}
