package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.membershipfees.FeeYearPublicationId;
import com.klabis.membershipfees.domain.FeeYearPublication;
import com.klabis.membershipfees.domain.FeeYearPublicationRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class FeeYearPublicationRepositoryAdapter implements FeeYearPublicationRepository {

    private final FeeYearPublicationJdbcRepository jdbcRepository;

    FeeYearPublicationRepositoryAdapter(FeeYearPublicationJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public FeeYearPublication save(FeeYearPublication publication) {
        return jdbcRepository.save(FeeYearPublicationMemento.from(publication)).toPublication();
    }

    @Override
    public Optional<FeeYearPublication> findById(FeeYearPublicationId id) {
        return jdbcRepository.findById(id.value()).map(FeeYearPublicationMemento::toPublication);
    }

    @Override
    public Optional<FeeYearPublication> findByYear(int year) {
        return jdbcRepository.findByYear(year).map(FeeYearPublicationMemento::toPublication);
    }

    @Override
    public List<FeeYearPublication> findAll() {
        List<FeeYearPublication> result = new ArrayList<>();
        jdbcRepository.findAll().forEach(m -> result.add(m.toPublication()));
        return result;
    }
}
