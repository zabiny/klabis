package com.klabis.groups.freegroup.application;

import com.klabis.groups.freegroup.domain.Invitation;
import com.klabis.groups.freegroup.FreeGroupId;

public record PendingInvitationView(FreeGroupId groupId, String groupName, Invitation invitation) {
}
