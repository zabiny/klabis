package com.klabis.groups.freegroup.infrastructure.restapi;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record InviteMemberRequest(@NotNull UUID memberId) {
}
