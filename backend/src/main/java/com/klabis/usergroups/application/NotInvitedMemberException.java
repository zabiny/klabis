package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.domain.InvitationId;

/**
 * @deprecated Moved to domain package. Will be removed in Phase 3. Use {@link com.klabis.usergroups.domain.NotInvitedMemberException} directly.
 */
@Deprecated
public class NotInvitedMemberException extends com.klabis.usergroups.domain.NotInvitedMemberException {

    public NotInvitedMemberException(MemberId memberId, InvitationId invitationId) {
        super(memberId, invitationId);
    }
}
