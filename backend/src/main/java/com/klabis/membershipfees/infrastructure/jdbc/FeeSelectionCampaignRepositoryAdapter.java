package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.FeeSelectionCampaignRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@SecondaryAdapter
@Repository
class FeeSelectionCampaignRepositoryAdapter implements FeeSelectionCampaignRepository {

    private final FeeSelectionCampaignJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    FeeSelectionCampaignRepositoryAdapter(FeeSelectionCampaignJdbcRepository jdbcRepository,
                                          JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
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
        Query query = Query.query(Criteria.where("publication_year").is(year));
        return jdbcAggregateTemplate.findOne(query, FeeSelectionCampaignMemento.class)
                .map(FeeSelectionCampaignMemento::toPublication);
    }

    @Override
    public List<FeeSelectionCampaign> findAll() {
        return StreamSupport.stream(jdbcRepository.findAll().spliterator(), false)
                .map(FeeSelectionCampaignMemento::toPublication)
                .toList();
    }

    @Override
    public List<FeeSelectionCampaign> findUnprocessedClosedPublications(LocalDate today) {
        Criteria criteria = Criteria.where("voting_deadline").lessThan(today)
                .and(Criteria.where("deadline_processed_at").isNull());
        Query query = Query.query(criteria);
        return jdbcAggregateTemplate.findAll(query, FeeSelectionCampaignMemento.class).stream()
                .map(FeeSelectionCampaignMemento::toPublication)
                .toList();
    }

    @Override
    public Optional<FeeSelectionCampaign> findActive(LocalDate today) {
        Criteria criteria = Criteria.where("voting_deadline").greaterThanOrEquals(today)
                .and(Criteria.where("deadline_processed_at").isNull());
        Query query = Query.query(criteria).limit(1);
        return jdbcAggregateTemplate.findOne(query, FeeSelectionCampaignMemento.class)
                .map(FeeSelectionCampaignMemento::toPublication);
    }
}
