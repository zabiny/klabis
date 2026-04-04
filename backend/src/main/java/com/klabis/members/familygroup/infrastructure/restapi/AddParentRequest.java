package com.klabis.members.familygroup.infrastructure.restapi;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record AddParentRequest(@NotNull UUID memberId) {
}
