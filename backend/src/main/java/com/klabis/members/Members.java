package com.klabis.members;

import com.klabis.common.users.UserId;

import java.util.Optional;

public interface Members {

    Optional<MemberDto> findByUserId(UserId memberId);

    Optional<MemberDto> findByRegistrationNumber(String registrationNumber);
}
