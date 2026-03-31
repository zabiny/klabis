package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.InvitationId;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.WithInvitations;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface InvitationPort {

    void inviteMember(UserGroupId groupId, MemberId invitedBy, MemberId target);

    void acceptInvitation(UserGroupId groupId, InvitationId invitationId, MemberId acceptingMember);

    void rejectInvitation(UserGroupId groupId, InvitationId invitationId, MemberId rejectingMember);

    <T extends UserGroup & WithInvitations> List<T> getGroupsWithPendingInvitations(MemberId memberId);
}
