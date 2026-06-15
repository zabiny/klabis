package com.klabis.membershipfees.domain;

import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Set;

@Repository
public interface YearlyFeeChargeMarkerRepository {

    boolean existsByMemberIdAndYear(MemberId memberId, int year);

    void markCharged(MemberId memberId, int year);

    Set<MemberId> findChargedMemberIdsForYear(int year);
}
