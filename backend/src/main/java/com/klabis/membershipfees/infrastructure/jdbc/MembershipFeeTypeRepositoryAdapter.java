package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.membershipfees.MembershipFeeTypeId;
import com.klabis.membershipfees.domain.MembershipFeeType;
import com.klabis.membershipfees.domain.MembershipFeeTypeRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class MembershipFeeTypeRepositoryAdapter implements MembershipFeeTypeRepository {

    private final MembershipFeeTypeJdbcRepository jdbcRepository;

    MembershipFeeTypeRepositoryAdapter(MembershipFeeTypeJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public MembershipFeeType save(MembershipFeeType level) {
        return jdbcRepository.save(MembershipFeeTypeMemento.from(level)).toType();
    }

    @Override
    public Optional<MembershipFeeType> findById(MembershipFeeTypeId id) {
        return jdbcRepository.findById(id.value()).map(MembershipFeeTypeMemento::toType);
    }

    @Override
    public List<MembershipFeeType> findAll() {
        return jdbcRepository.findAll().stream()
                .map(MembershipFeeTypeMemento::toType)
                .toList();
    }

    @Override
    public void delete(MembershipFeeTypeId id) {
        jdbcRepository.deleteById(id.value());
    }
}
