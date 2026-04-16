package com.klabis.members.membersgroup.application;

import com.klabis.common.usergroup.Invitation;
import com.klabis.members.membersgroup.domain.MembersGroupId;

public record PendingInvitationView(MembersGroupId groupId, String groupName, Invitation invitation) {
}
