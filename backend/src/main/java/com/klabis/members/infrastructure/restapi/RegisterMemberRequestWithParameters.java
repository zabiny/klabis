package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;

record RegisterMemberRequestWithParameters(RegisterMemberRequest request, UserId registeredBy) {
}
