package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.MembershipFeeLevel;
import com.klabis.membershipfees.domain.MembershipFeeLevelRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class MembershipFeeLevelRepositoryAdapter implements MembershipFeeLevelRepository {

    private final MembershipFeeLevelJdbcRepository jdbcRepository;

    MembershipFeeLevelRepositoryAdapter(MembershipFeeLevelJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public MembershipFeeLevel save(MembershipFeeLevel level) {
        return jdbcRepository.save(MembershipFeeLevelMemento.from(level)).toLevel();
    }

    @Override
    public Optional<MembershipFeeLevel> findById(MembershipFeeLevelId id) {
        return jdbcRepository.findById(id.value()).map(MembershipFeeLevelMemento::toLevel);
    }

    @Override
    public List<MembershipFeeLevel> findAll() {
        return jdbcRepository.findAll().stream()
                .map(MembershipFeeLevelMemento::toLevel)
                .toList();
    }

    @Override
    public void delete(MembershipFeeLevelId id) {
        jdbcRepository.deleteById(id.value());
    }
}
