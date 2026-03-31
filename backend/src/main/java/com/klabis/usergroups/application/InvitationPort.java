package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.InvitationId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface InvitationPort {

    void inviteMember(UserGroupId groupId, MemberId invitedBy, MemberId target);

    void acceptInvitation(UserGroupId groupId, InvitationId invitationId, MemberId acceptingMember);

    void rejectInvitation(UserGroupId groupId, InvitationId invitationId, MemberId rejectingMember);

    List<FreeGroup> getGroupsWithPendingInvitations(MemberId memberId);
}
