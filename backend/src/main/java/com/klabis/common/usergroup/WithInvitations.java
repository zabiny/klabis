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

    void addOwner(UserId userId);

    /**
     * Promotes an existing member to owner. Enforces the invitation-group invariant:
     * only members who have already joined through the invitation flow may become owners.
     * Delegates to {@code addOwner} once the check passes. Because the caller already
     * asserted the user is a member, this path never needs to add a membership entry.
     */
    default void promoteOwner(UserId userId) {
        if (!hasMember(userId)) {
            throw new CannotPromoteNonMemberToOwnerException(userId);
        }
        addOwner(userId);
    }
}
