package com.klabis.members.membersgroup.infrastructure.jdbc;

import com.klabis.members.MemberId;
import com.klabis.members.groups.infrastructure.jdbc.GroupJdbcRepository;
import com.klabis.members.groups.infrastructure.jdbc.GroupMemento;
import com.klabis.members.membersgroup.domain.MembersGroup;
import com.klabis.members.membersgroup.domain.MembersGroupId;
import com.klabis.members.membersgroup.domain.MembersGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class MembersGroupRepositoryAdapter implements MembersGroupRepository {

    private final GroupJdbcRepository jdbcRepository;

    MembersGroupRepositoryAdapter(GroupJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public MembersGroup save(MembersGroup group) {
        return jdbcRepository.save(GroupMemento.fromMembersGroup(group)).toMembersGroup();
    }

    @Override
    public Optional<MembersGroup> findById(MembersGroupId id) {
        return jdbcRepository.findByIdAndType(id.value(), MembersGroup.TYPE_DISCRIMINATOR)
                .map(GroupMemento::toMembersGroup);
    }

    @Override
    public List<MembersGroup> findGroupsForMember(MemberId memberId) {
        return jdbcRepository.findOwnersOrMembersByType(memberId.value(), MembersGroup.TYPE_DISCRIMINATOR)
                .stream().map(GroupMemento::toMembersGroup).toList();
    }

    @Override
    public List<MembersGroup> findGroupsWithPendingInvitationsForMember(MemberId memberId) {
        return jdbcRepository.findWithPendingInvitationsByType(memberId.value(), MembersGroup.TYPE_DISCRIMINATOR)
                .stream().map(GroupMemento::toMembersGroup).toList();
    }

    @Override
    public boolean existsById(MembersGroupId id) {
        return jdbcRepository.existsByIdAndType(id.value(), MembersGroup.TYPE_DISCRIMINATOR);
    }

    @Override
    public void delete(MembersGroupId id) {
        jdbcRepository.deleteByIdAndType(id.value(), MembersGroup.TYPE_DISCRIMINATOR);
    }
}
