package com.klabis.groups.freegroup.application;

import com.klabis.common.usergroup.Invitation;
import com.klabis.groups.freegroup.FreeGroupId;

public record PendingInvitationView(FreeGroupId groupId, String groupName, Invitation invitation) {
}
