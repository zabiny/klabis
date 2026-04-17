package com.klabis.groups.familygroup.infrastructure.restapi;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record AddMemberRequest(@NotNull UUID memberId) {
}
