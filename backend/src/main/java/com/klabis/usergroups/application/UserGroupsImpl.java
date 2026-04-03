package com.klabis.usergroups.application;

import com.klabis.members.FamilyGroupProvider;
import com.klabis.members.LastOwnershipChecker;
import com.klabis.members.MemberId;
import com.klabis.members.TrainingGroupProvider;
import com.klabis.usergroups.UserGroupOwnershipInfo;
import com.klabis.usergroups.UserGroups;
import com.klabis.usergroups.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class UserGroupsImpl implements UserGroups, LastOwnershipChecker, TrainingGroupProvider, FamilyGroupProvider {

    private final UserGroupRepository userGroupRepository;

    UserGroupsImpl(UserGroupRepository userGroupRepository) {
        this.userGroupRepository = userGroupRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserGroupOwnershipInfo> findGroupsWhereLastOwner(MemberId memberId) {
        return findSolelyOwnedGroups(memberId).stream()
                .map(group -> new UserGroupOwnershipInfo(
                        group.getId().uuid(),
                        group.getName(),
                        group.typeDiscriminator()))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<OwnedGroupInfo> findGroupsOwnedSolely(MemberId memberId) {
        return findSolelyOwnedGroups(memberId).stream()
                .map(group -> new OwnedGroupInfo(
                        group.getId().uuid().toString(),
                        group.getName(),
                        group.typeDiscriminator()))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<TrainingGroupProvider.TrainingGroupData> findTrainingGroupForMember(MemberId memberId) {
        return userGroupRepository.findAllByMember(memberId).stream()
                .filter(group -> group instanceof TrainingGroup)
                .map(group -> (TrainingGroup) group)
                .findFirst()
                .map(group -> new TrainingGroupProvider.TrainingGroupData(group.getName(), group.getOwners()));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<FamilyGroupProvider.FamilyGroupData> findFamilyGroupForMember(MemberId memberId) {
        return userGroupRepository.findAllByMember(memberId).stream()
                .filter(group -> group instanceof FamilyGroup)
                .map(group -> (FamilyGroup) group)
                .findFirst()
                .map(group -> new FamilyGroupProvider.FamilyGroupData(group.getName(), group.getOwners()));
    }

    private List<UserGroup> findSolelyOwnedGroups(MemberId memberId) {
        return userGroupRepository.findAllByOwner(memberId).stream()
                .filter(group -> group.isLastOwner(memberId))
                .toList();
    }

}
