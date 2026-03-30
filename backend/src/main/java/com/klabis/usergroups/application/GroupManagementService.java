package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class GroupManagementService implements GroupManagementPort {

    private final UserGroupRepository userGroupRepository;

    GroupManagementService(UserGroupRepository userGroupRepository) {
        this.userGroupRepository = userGroupRepository;
    }

    @Transactional
    @Override
    public UserGroup createFreeGroup(FreeGroup.CreateFreeGroup command) {
        FreeGroup group = FreeGroup.create(command);
        return userGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    @Override
    public UserGroup getGroup(UserGroupId id) {
        return userGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserGroup> listGroupsForMember(MemberId memberId) {
        return userGroupRepository.findAllByMember(memberId);
    }

    @Transactional
    @Override
    public UserGroup renameGroup(UserGroupId id, String newName, MemberId requestingMember) {
        UserGroup group = loadGroup(id);
        requireOwner(group, requestingMember);
        group.rename(newName);
        return userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void deleteGroup(UserGroupId id, MemberId requestingMember) {
        UserGroup group = loadGroup(id);
        requireOwner(group, requestingMember);
        userGroupRepository.delete(id);
    }

    @Transactional
    @Override
    public UserGroup addMemberToGroup(UserGroupId id, MemberId memberToAdd, MemberId requestingMember) {
        UserGroup group = loadGroup(id);
        requireOwner(group, requestingMember);
        group.addMember(memberToAdd);
        return userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public UserGroup removeMemberFromGroup(UserGroupId id, MemberId memberToRemove, MemberId requestingMember) {
        UserGroup group = loadGroup(id);
        requireOwner(group, requestingMember);
        group.removeMember(memberToRemove);
        return userGroupRepository.save(group);
    }

    private UserGroup loadGroup(UserGroupId id) {
        return userGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
    }

    private void requireOwner(UserGroup group, MemberId requestingMember) {
        if (!group.isOwner(requestingMember)) {
            throw new NotGroupOwnerException(requestingMember, group.getId());
        }
    }
}
