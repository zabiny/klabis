package com.klabis.membershipfees.domain;

import com.klabis.membershipfees.MembershipFeeGroupId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipFeeGroupRepository {

    MembershipFeeGroup save(MembershipFeeGroup group);

    Optional<MembershipFeeGroup> findById(MembershipFeeGroupId id);

    List<MembershipFeeGroup> findByYear(int year);
}
