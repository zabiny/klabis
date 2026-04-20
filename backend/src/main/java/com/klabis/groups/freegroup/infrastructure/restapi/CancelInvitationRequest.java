package com.klabis.groups.freegroup.infrastructure.restapi;

import jakarta.validation.constraints.Size;

record CancelInvitationRequest(@Size(max = 500) String reason) {
}
