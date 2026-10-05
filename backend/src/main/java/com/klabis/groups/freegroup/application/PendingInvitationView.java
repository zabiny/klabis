package com.klabis.groups.freegroup.application;

import com.klabis.common.users.Authority;
import com.klabis.groups.freegroup.domain.Invitation;
import com.klabis.groups.freegroup.FreeGroupId;

import java.util.Set;

public record PendingInvitationView(FreeGroupId groupId, String groupName, Invitation invitation,
                                    Set<Authority> delegatedAuthorities) {
}
