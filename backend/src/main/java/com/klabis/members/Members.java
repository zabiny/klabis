package com.klabis.members;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@PrimaryPort
public interface Members {

    Optional<MemberDto> findById(MemberId memberId);

    Map<MemberId, MemberDto> findByIds(Collection<MemberId> memberIds);

    Map<MemberId, MemberAccommodationDto> findAccommodationDataByIds(Collection<MemberId> memberIds);

    Optional<MemberDto> findByRegistrationNumber(String registrationNumber);
}
