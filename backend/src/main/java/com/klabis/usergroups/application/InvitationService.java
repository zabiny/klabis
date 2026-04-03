package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.GroupFilter;
import com.klabis.usergroups.domain.InvitationId;
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
        FreeGroup group = loadGroupWithInvitations(groupId);
        group.inviteMember(new FreeGroup.Invite(invitedBy, target));
        userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void acceptInvitation(UserGroupId groupId, InvitationId invitationId, MemberId acceptingMember) {
        FreeGroup group = loadGroupWithInvitations(groupId);
        group.acceptInvitation(new FreeGroup.AcceptInvitation(invitationId, acceptingMember));
        userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void rejectInvitation(UserGroupId groupId, InvitationId invitationId, MemberId rejectingMember) {
        FreeGroup group = loadGroupWithInvitations(groupId);
        group.rejectInvitation(new FreeGroup.RejectInvitation(invitationId, rejectingMember));
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

    private FreeGroup loadGroupWithInvitations(UserGroupId groupId) {
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
        if (!(group instanceof FreeGroup freeGroup)) {
            throw new GroupNotFoundException(groupId);
        }
        return freeGroup;
    }
}
