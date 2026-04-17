package com.klabis.groups.membersgroup.application;

import com.klabis.common.usergroup.Invitation;
import com.klabis.groups.membersgroup.MembersGroupId;

public record PendingInvitationView(MembersGroupId groupId, String groupName, Invitation invitation) {
}
