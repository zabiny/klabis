package com.klabis.usergroups.application;

import com.klabis.members.FamilyGroupProvider;
import com.klabis.members.LastOwnershipChecker;
import com.klabis.members.MemberId;
import com.klabis.members.TrainingGroupProvider;
import com.klabis.usergroups.UserGroupOwnershipInfo;
import com.klabis.usergroups.UserGroups;
import com.klabis.usergroups.domain.FamilyGroup;
import com.klabis.usergroups.domain.GroupFilter;
import com.klabis.usergroups.domain.GroupType;
import com.klabis.members.LastOwnershipChecker.OwnedGroupInfo;
import com.klabis.usergroups.domain.TrainingGroup;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
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
        return userGroupRepository.findOne(GroupFilter.byTypeAndMember(GroupType.TRAINING, memberId))
                .map(group -> (TrainingGroup) group)
                .map(group -> new TrainingGroupProvider.TrainingGroupData(group.getId().uuid()));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<FamilyGroupProvider.FamilyGroupData> findFamilyGroupForMember(MemberId memberId) {
        return userGroupRepository.findOne(GroupFilter.byTypeAndMember(GroupType.FAMILY, memberId))
                .map(group -> (FamilyGroup) group)
                .map(group -> new FamilyGroupProvider.FamilyGroupData(group.getId().uuid()));
    }

    private List<UserGroup> findSolelyOwnedGroups(MemberId memberId) {
        return userGroupRepository.findAll(GroupFilter.byOwner(memberId)).stream()
                .filter(group -> group.isLastOwner(memberId))
                .toList();
    }

}
