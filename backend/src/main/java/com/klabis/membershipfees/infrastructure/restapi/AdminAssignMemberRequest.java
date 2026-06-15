package com.klabis.membershipfees.infrastructure.restapi;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record AdminAssignMemberRequest(@NotNull UUID memberId, @NotNull Integer year) {
}
