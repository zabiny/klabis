package com.klabis.usergroups.domain;

import com.klabis.members.MemberId;

import java.util.List;
import java.util.Set;

public interface WithInvitations {

    void invite(MemberId invitedBy, MemberId target);

    void acceptInvitation(InvitationId invitationId);

    void rejectInvitation(InvitationId invitationId);

    List<Invitation> getPendingInvitations();

    Set<Invitation> getInvitations();

    boolean isInvitedMember(InvitationId invitationId, MemberId memberId);
}
