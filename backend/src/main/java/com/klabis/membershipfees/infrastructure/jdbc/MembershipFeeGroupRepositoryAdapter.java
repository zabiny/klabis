package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class MembershipFeeGroupRepositoryAdapter implements MembershipFeeGroupRepository {

    private final MembershipFeeGroupJdbcRepository jdbcRepository;

    MembershipFeeGroupRepositoryAdapter(MembershipFeeGroupJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public MembershipFeeGroup save(MembershipFeeGroup group) {
        return jdbcRepository.save(MembershipFeeGroupMemento.from(group)).toGroup();
    }

    @Override
    public Optional<MembershipFeeGroup> findById(MembershipFeeGroupId id) {
        return jdbcRepository.findById(id.value()).map(MembershipFeeGroupMemento::toGroup);
    }

    @Override
    public List<MembershipFeeGroup> findByYear(int year) {
        return jdbcRepository.findByYear(year).stream()
                .map(MembershipFeeGroupMemento::toGroup)
                .toList();
    }

    @Override
    public List<MembershipFeeGroup> saveAll(List<MembershipFeeGroup> groups) {
        return groups.stream().map(this::save).toList();
    }

    @Override
    public Optional<MembershipFeeGroup> findByMemberAndYear(MemberId memberId, int year) {
        return jdbcRepository.findByMemberAndYear(memberId.value(), year)
                .map(MembershipFeeGroupMemento::toGroup);
    }
}
