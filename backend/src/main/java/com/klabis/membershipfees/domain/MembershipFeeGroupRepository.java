package com.klabis.membershipfees.domain;

import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipFeeGroupRepository {

    MembershipFeeGroup save(MembershipFeeGroup group);

    Optional<MembershipFeeGroup> findById(MembershipFeeGroupId id);

    List<MembershipFeeGroup> findByYear(int year);

    List<MembershipFeeGroup> saveAll(List<MembershipFeeGroup> groups);

    Optional<MembershipFeeGroup> findByMemberAndYear(MemberId memberId, int year);

    List<MembershipFeeGroup> findByMember(MemberId memberId);

    boolean existsByYearAndSourceLevelId(int year, MembershipFeeLevelId sourceLevelId);
}
