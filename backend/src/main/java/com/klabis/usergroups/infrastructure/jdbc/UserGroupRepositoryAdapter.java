package com.klabis.usergroups.infrastructure.jdbc;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.FamilyGroup;
import com.klabis.usergroups.domain.TrainingGroup;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class UserGroupRepositoryAdapter implements UserGroupRepository {

    private final UserGroupJdbcRepository jdbcRepository;

    UserGroupRepositoryAdapter(UserGroupJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public UserGroup save(UserGroup userGroup) {
        return jdbcRepository.save(UserGroupMemento.from(userGroup)).toUserGroup();
    }

    @Override
    public Optional<UserGroup> findById(UserGroupId id) {
        return jdbcRepository.findById(id.uuid()).map(UserGroupMemento::toUserGroup);
    }

    @Override
    public List<UserGroup> findAllByMember(MemberId memberId) {
        return jdbcRepository.findAllByMemberId(memberId.uuid()).stream()
                .map(UserGroupMemento::toUserGroup)
                .toList();
    }

    @Override
    public List<UserGroup> findAllByOwner(MemberId memberId) {
        return jdbcRepository.findAllByOwnerId(memberId.uuid()).stream()
                .map(UserGroupMemento::toUserGroup)
                .toList();
    }

    @Override
    public List<UserGroup> findAllWithPendingInvitationForMember(MemberId memberId) {
        return jdbcRepository.findAllWithPendingInvitationForMember(memberId.uuid()).stream()
                .map(UserGroupMemento::toUserGroup)
                .toList();
    }

    @Override
    public List<TrainingGroup> findAllTrainingGroups() {
        return jdbcRepository.findAllTrainingGroups().stream()
                .map(m -> (TrainingGroup) m.toUserGroup())
                .toList();
    }

    @Override
    public List<FamilyGroup> findAllFamilyGroups() {
        return jdbcRepository.findAllFamilyGroups().stream()
                .map(m -> (FamilyGroup) m.toUserGroup())
                .toList();
    }

    @Override
    public Optional<FamilyGroup> findFamilyGroupByMember(MemberId memberId) {
        return jdbcRepository.findFamilyGroupsByMemberId(memberId.uuid()).stream()
                .findFirst()
                .map(m -> (FamilyGroup) m.toUserGroup());
    }

    @Override
    public void delete(UserGroupId id) {
        jdbcRepository.deleteById(id.uuid());
    }
}
