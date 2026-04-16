package com.klabis.members.membersgroup.infrastructure.jdbc;

import com.klabis.members.MemberId;
import com.klabis.members.membersgroup.domain.MembersGroup;
import com.klabis.members.membersgroup.domain.MembersGroupId;
import com.klabis.members.membersgroup.domain.MembersGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class MembersGroupRepositoryAdapter implements MembersGroupRepository {

    private final MembersGroupJdbcRepository jdbcRepository;

    MembersGroupRepositoryAdapter(MembersGroupJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public MembersGroup save(MembersGroup group) {
        return jdbcRepository.save(MembersGroupMemento.from(group)).toMembersGroup();
    }

    @Override
    public Optional<MembersGroup> findById(MembersGroupId id) {
        return jdbcRepository.findById(id.value()).map(MembersGroupMemento::toMembersGroup);
    }

    @Override
    public List<MembersGroup> findGroupsForMember(MemberId memberId) {
        List<MembersGroup> result = new ArrayList<>();
        jdbcRepository.findGroupsForMember(memberId.value()).forEach(m -> result.add(m.toMembersGroup()));
        return result;
    }

    @Override
    public List<MembersGroup> findGroupsWithPendingInvitationsForMember(MemberId memberId) {
        List<MembersGroup> result = new ArrayList<>();
        jdbcRepository.findGroupsWithPendingInvitationsForMember(memberId.value())
                .forEach(m -> result.add(m.toMembersGroup()));
        return result;
    }

    @Override
    public boolean existsById(MembersGroupId id) {
        return jdbcRepository.existsById(id.value());
    }

    @Override
    public void delete(MembersGroupId id) {
        jdbcRepository.deleteById(id.value());
    }
}
