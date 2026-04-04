package com.klabis.members.membersgroup.infrastructure.restapi;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record InviteMemberRequest(@NotNull UUID memberId) {
}
