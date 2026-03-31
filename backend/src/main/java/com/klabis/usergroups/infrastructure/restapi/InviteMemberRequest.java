package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.members.MemberId;
import jakarta.validation.constraints.NotNull;

record InviteMemberRequest(@NotNull MemberId memberId) {
}
