package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.InvitationId;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class InvitationService implements InvitationPort {

    private final UserGroupRepository userGroupRepository;

    InvitationService(UserGroupRepository userGroupRepository) {
        this.userGroupRepository = userGroupRepository;
    }

    @Transactional
    @Override
    public void inviteMember(UserGroupId groupId, MemberId invitedBy, MemberId target) {
        FreeGroup group = loadFreeGroup(groupId);
        requireOwner(group, invitedBy);
        group.invite(invitedBy, target);
        userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void acceptInvitation(UserGroupId groupId, InvitationId invitationId, MemberId acceptingMember) {
        FreeGroup group = loadFreeGroup(groupId);
        requireInvitedMember(group, invitationId, acceptingMember);
        group.acceptInvitation(invitationId);
        userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void rejectInvitation(UserGroupId groupId, InvitationId invitationId, MemberId rejectingMember) {
        FreeGroup group = loadFreeGroup(groupId);
        requireInvitedMember(group, invitationId, rejectingMember);
        group.rejectInvitation(invitationId);
        userGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    @Override
    public List<FreeGroup> getGroupsWithPendingInvitations(MemberId memberId) {
        return userGroupRepository.findAllWithPendingInvitationForMember(memberId).stream()
                .filter(FreeGroup.class::isInstance)
                .map(FreeGroup.class::cast)
                .toList();
    }

    private FreeGroup loadFreeGroup(UserGroupId groupId) {
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
        if (!(group instanceof FreeGroup freeGroup)) {
            throw new GroupNotFoundException(groupId);
        }
        return freeGroup;
    }

    private void requireOwner(FreeGroup group, MemberId requestingMember) {
        if (!group.isOwner(requestingMember)) {
            throw new NotGroupOwnerException(requestingMember, group.getId());
        }
    }

    private void requireInvitedMember(FreeGroup group, InvitationId invitationId, MemberId member) {
        if (!group.isInvitedMember(invitationId, member)) {
            throw new NotInvitedMemberException(member, invitationId);
        }
    }
}
