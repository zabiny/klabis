package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.GroupFilter;
import com.klabis.usergroups.domain.InvitationId;
import com.klabis.usergroups.domain.NotGroupOwnerException;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import com.klabis.usergroups.domain.WithInvitations;
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
        var group = loadGroupWithInvitations(groupId);
        if (!group.isOwner(invitedBy)) {
            throw new NotGroupOwnerException(invitedBy, group.getId());
        }
        group.invite(invitedBy, target);
        userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void acceptInvitation(UserGroupId groupId, InvitationId invitationId, MemberId acceptingMember) {
        var group = loadGroupWithInvitations(groupId);
        requireInvitedMember(group, invitationId, acceptingMember);
        group.acceptInvitation(invitationId);
        userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void rejectInvitation(UserGroupId groupId, InvitationId invitationId, MemberId rejectingMember) {
        var group = loadGroupWithInvitations(groupId);
        requireInvitedMember(group, invitationId, rejectingMember);
        group.rejectInvitation(invitationId);
        userGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public <T extends UserGroup & WithInvitations> List<T> getGroupsWithPendingInvitations(MemberId memberId) {
        return userGroupRepository.findAll(GroupFilter.byPendingInvitation(memberId)).stream()
                .filter(WithInvitations.class::isInstance)
                .map(g -> (T) g)
                .toList();
    }

    private <T extends UserGroup & WithInvitations> T loadGroupWithInvitations(UserGroupId groupId) {
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
        if (!(group instanceof WithInvitations)) {
            throw new GroupNotFoundException(groupId);
        }
        @SuppressWarnings("unchecked")
        T result = (T) group;
        return result;
    }

    private void requireInvitedMember(WithInvitations group, InvitationId invitationId, MemberId member) {
        if (!group.isInvitedMember(invitationId, member)) {
            throw new NotInvitedMemberException(member, invitationId);
        }
    }
}
