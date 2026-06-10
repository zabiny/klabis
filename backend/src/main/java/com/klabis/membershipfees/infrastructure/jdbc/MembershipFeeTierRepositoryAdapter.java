package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.MembershipFeeTier;
import com.klabis.membershipfees.domain.MembershipFeeTierRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class MembershipFeeTierRepositoryAdapter implements MembershipFeeTierRepository {

    private final MembershipFeeTierJdbcRepository jdbcRepository;

    MembershipFeeTierRepositoryAdapter(MembershipFeeTierJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public MembershipFeeTier save(MembershipFeeTier level) {
        return jdbcRepository.save(MembershipFeeTierMemento.from(level)).toLevel();
    }

    @Override
    public Optional<MembershipFeeTier> findById(MembershipFeeTierId id) {
        return jdbcRepository.findById(id.value()).map(MembershipFeeTierMemento::toLevel);
    }

    @Override
    public List<MembershipFeeTier> findAll() {
        return jdbcRepository.findAll().stream()
                .map(MembershipFeeTierMemento::toLevel)
                .toList();
    }

    @Override
    public void delete(MembershipFeeTierId id) {
        jdbcRepository.deleteById(id.value());
    }
}
