package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.AgeRange;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.TrainingGroup;
import com.klabis.usergroups.domain.UserGroup;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface GroupManagementPort {

    UserGroup createFreeGroup(FreeGroup.CreateFreeGroup command);

    TrainingGroup createTrainingGroup(TrainingGroup.CreateTrainingGroup command);

    TrainingGroup updateTrainingGroupAgeRange(UserGroupId id, AgeRange newAgeRange, MemberId requestingMember);

    List<TrainingGroup> listTrainingGroups();

    TrainingGroup getTrainingGroup(UserGroupId id);

    UserGroup getGroup(UserGroupId id);

    List<UserGroup> listGroupsForMember(MemberId memberId);

    UserGroup renameGroup(UserGroupId id, String newName, MemberId requestingMember);

    void deleteGroup(UserGroupId id, MemberId requestingMember);

    UserGroup addMemberToGroup(UserGroupId id, MemberId memberToAdd, MemberId requestingMember);

    UserGroup removeMemberFromGroup(UserGroupId id, MemberId memberToRemove, MemberId requestingMember);
}
