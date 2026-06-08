package com.klabis.membershipfees.infrastructure.restapi;

import jakarta.validation.constraints.NotNull;

record AdminAssignMemberRequest(@NotNull Integer year) {
}
