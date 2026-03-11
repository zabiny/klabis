package com.klabis.members;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface Members {

    Optional<MemberDto> findById(MemberId memberId);

    Map<MemberId, MemberDto> findByIds(Collection<MemberId> memberIds);

    Optional<MemberDto> findByRegistrationNumber(String registrationNumber);
}
