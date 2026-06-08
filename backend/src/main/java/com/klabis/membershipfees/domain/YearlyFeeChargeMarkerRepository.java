package com.klabis.membershipfees.domain;

import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Repository;

@Repository
public interface YearlyFeeChargeMarkerRepository {

    boolean existsByMemberIdAndYear(MemberId memberId, int year);

    void markCharged(MemberId memberId, int year);
}
