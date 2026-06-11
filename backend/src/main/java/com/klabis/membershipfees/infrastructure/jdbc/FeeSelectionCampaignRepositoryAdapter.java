package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.FeeSelectionCampaignRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@SecondaryAdapter
@Repository
class FeeSelectionCampaignRepositoryAdapter implements FeeSelectionCampaignRepository {

    private final FeeSelectionCampaignJdbcRepository jdbcRepository;

    FeeSelectionCampaignRepositoryAdapter(FeeSelectionCampaignJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public FeeSelectionCampaign save(FeeSelectionCampaign publication) {
        return jdbcRepository.save(FeeSelectionCampaignMemento.from(publication)).toPublication();
    }

    @Override
    public Optional<FeeSelectionCampaign> findById(FeeSelectionCampaignId id) {
        return jdbcRepository.findById(id.value()).map(FeeSelectionCampaignMemento::toPublication);
    }

    @Override
    public Optional<FeeSelectionCampaign> findByYear(int year) {
        return jdbcRepository.findByYear(year).map(FeeSelectionCampaignMemento::toPublication);
    }

    @Override
    public List<FeeSelectionCampaign> findAll() {
        return StreamSupport.stream(jdbcRepository.findAll().spliterator(), false)
                .map(FeeSelectionCampaignMemento::toPublication)
                .toList();
    }

    @Override
    public List<FeeSelectionCampaign> findUnprocessedClosedPublications(LocalDate today) {
        return jdbcRepository.findUnprocessedClosedPublications(today).stream()
                .map(FeeSelectionCampaignMemento::toPublication)
                .toList();
    }

    @Override
    public Optional<FeeSelectionCampaign> findActive(LocalDate today) {
        return jdbcRepository.findActive(today).map(FeeSelectionCampaignMemento::toPublication);
    }
}
