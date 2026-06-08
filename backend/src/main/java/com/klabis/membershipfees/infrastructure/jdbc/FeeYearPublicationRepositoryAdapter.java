package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.membershipfees.FeeYearPublicationId;
import com.klabis.membershipfees.domain.FeeYearPublication;
import com.klabis.membershipfees.domain.FeeYearPublicationRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

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
        return StreamSupport.stream(jdbcRepository.findAll().spliterator(), false)
                .map(FeeYearPublicationMemento::toPublication)
                .toList();
    }

    @Override
    public List<FeeYearPublication> findUnprocessedClosedPublications(LocalDate today) {
        return jdbcRepository.findUnprocessedClosedPublications(today).stream()
                .map(FeeYearPublicationMemento::toPublication)
                .toList();
    }
}
