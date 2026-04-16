package com.klabis.common.usergroup;

import com.klabis.common.users.UserId;

import java.util.List;
import java.util.Set;

public interface WithInvitations {

    void invite(UserId invitedBy, UserId target);

    void acceptInvitation(InvitationId invitationId);

    void rejectInvitation(InvitationId invitationId);

    List<Invitation> getPendingInvitations();

    Set<Invitation> getInvitations();

    boolean isInvitedMember(UserId userId);

    boolean hasMember(UserId userId);
}
