package com.klabis.members.membersgroup.infrastructure.restapi;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record AddOwnerRequest(@NotNull UUID memberId) {
}
