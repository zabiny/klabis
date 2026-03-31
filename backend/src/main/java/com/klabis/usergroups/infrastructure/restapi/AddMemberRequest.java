package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.members.MemberId;
import jakarta.validation.constraints.NotNull;

record AddMemberRequest(@NotNull MemberId memberId) {
}
