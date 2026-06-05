package com.klabis.membershipfees.domain;

import com.klabis.membershipfees.MembershipFeeLevelId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;
import java.util.Optional;

@SecondaryPort
public interface MembershipFeeLevelRepository {

    MembershipFeeLevel save(MembershipFeeLevel level);

    Optional<MembershipFeeLevel> findById(MembershipFeeLevelId id);

    List<MembershipFeeLevel> findAll();

    void delete(MembershipFeeLevelId id);
}
