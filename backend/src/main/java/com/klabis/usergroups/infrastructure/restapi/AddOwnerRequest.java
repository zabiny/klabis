package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.members.MemberId;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record AddOwnerRequest(@NotNull UUID memberId) {

    MemberId toMemberId() {
        return new MemberId(memberId);
    }
}
