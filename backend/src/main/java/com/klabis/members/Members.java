package com.klabis.members;

import java.util.Optional;

public interface Members {

    Optional<MemberDto> findById(MemberId memberId);

    Optional<MemberDto> findByRegistrationNumber(String registrationNumber);
}
